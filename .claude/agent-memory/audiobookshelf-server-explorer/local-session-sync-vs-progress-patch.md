---
name: local-session-sync-vs-progress-patch
description: Precise mechanics of the recency guard in POST /api/session/local(-all) vs the unconditional PATCH /api/me/progress/:id — why the PATCH is not redundant. Source+live verified v2.34.0-4-ge39e8d8c.
metadata:
  type: reference
---

Verified against `/Users/r0adkll/OpenSource/audiobookshelf` @ `v2.34.0-4-ge39e8d8c` (container source, matches [[playback-session-api]]). Refines the recency-guard claim in that memory with exact line numbers/behavior.

## Route -> handler chain
- `server/routers/ApiRouter.js:233-234` — `POST /session/local` -> `SessionController.syncLocal`, `POST /session/local-all` -> `SessionController.syncLocalSessions`.
- `server/controllers/SessionController.js:254-268` — both just delegate to `PlaybackSessionManager`.
- `server/managers/PlaybackSessionManager.js:103-118` (`syncLocalSessionsRequest`, bulk) and `:281-290` (`syncLocalSessionRequest`, single) both call the **same** `syncLocalSession(user, sessionJson, deviceInfo)` at `:127-274` per session — single vs bulk share one write path, no divergence.
- Both ultimately call `user.createUpdateMediaProgressFromPayload` (`server/models/User.js:731`), same as the live `/api/session/:id/sync` path (`PlaybackSessionManager.js:391` inside `syncSession`).

## Recency guard — exact mechanics (PlaybackSessionManager.js:229-247)
- Only runs when a MediaProgress row **already exists** for the item (`userProgressForItem` truthy, `:231`). A brand-new progress row is always created unconditionally (`:248-261`), no guard.
- Condition (`:232`): `userProgressForItem.updatedAt.valueOf() > session.updatedAt` -> skip, log-only (`:233`), `result.progressSynced` stays `false`. No error surfaced.
- `session.updatedAt` is the **client-supplied `sessionJson.updatedAt` field**, not server time: set at `:210` for an existing local session row, or via `PlaybackSession` constructor `this.updatedAt = session.updatedAt || session.startedAt` (`objects/PlaybackSession.js:162`) for a new one.
- This guard applies identically whether reached via `/session/local` or `/session/local-all` (same `syncLocalSession` call). The **live** `/session/:id/sync` -> `syncSession` (`:373-412`) has **no such guard** — it always calls `createUpdateMediaProgressFromPayload` unconditionally.

## Where the client-controlled clock persists (the key subtlety)
- `session.mediaProgressObject` (`objects/PlaybackSession.js:191-198`) includes `lastUpdate: this.updatedAt` — i.e. the local session's client-supplied updatedAt gets forwarded into the progress-update payload.
- `syncSession` (live path) does **NOT** include `lastUpdate` in its payload (`PlaybackSessionManager.js:391-400`) — only local sync does.
- `MediaProgress.applyProgressUpdate` (`models/MediaProgress.js:190-267`) — after `.save()` (which sets the DB `updatedAt` column to real server "now" via Sequelize timestamps), there's a **second step** (`:252-264`): if `progressPayload.lastUpdate` is present, it runs a raw SQL `UPDATE mediaProgresses SET updatedAt = <client value> WHERE id = ...` and reloads. **This only fires on the UPDATE branch (existing row)** — the CREATE branch (`User.js:797-827`, no existing mediaProgress) never reads `progressPayload.lastUpdate` at all, so a first-ever local sync for an item gets a real server timestamp, live-confirmed (payload updatedAt vs resulting `lastUpdate` differed by ~200ms on create, but matched **exactly** on a subsequent update).
- Net effect: after the *first* local sync creates a progress row, every guard check thereafter compares the *client's own clock* (stored via the raw-SQL override) against the *client's own next payload* — the guard is really "is this local session's updatedAt >= the last local session's updatedAt that won," which is meaningless as a staleness check across devices with clock skew, and is silently bypassed by a PATCH (PATCH never sets `lastUpdate`, so it always stores real server time).

## PATCH /api/me/progress/:libraryItemId/:episodeId? (MeController.js:155-168)
- Spreads the **entire raw `req.body`** into the payload passed to `createUpdateMediaProgressFromPayload` (`:156-160`) — no allowlist, no recency guard, always applied.
- Accepts fields a session-sync payload has no way to set: `isFinished` (explicit finish/unfinish toggle — unfinishing resets `currentTime` to 0, `MediaProgress.js:198-205`), `ebookLocation`, `ebookProgress`, `hideFromContinueListening`. Session-sync's `mediaProgressObject` only ever sends `duration`, `currentTime`, `progress`, `lastUpdate` — no ebook fields, no explicit `isFinished` (both session paths rely on auto mark-as-finished from `markAsFinishedTimeRemaining`/`markAsFinishedPercentComplete`, same logic for local and live).
- Because the PATCH body has no `lastUpdate`/`updatedAt` key, `applyProgressUpdate`'s raw-SQL override never fires for PATCH — the resulting `mediaProgresses.updatedAt` is always real server "now". Live-confirmed: PATCH with `currentTime` "older" than what a local-session recency guard would have rejected applied immediately (unconditional).

## Response-shape asymmetry (client-visibility gap)
- `POST /session/local` -> `res.sendStatus(200)` only (`PlaybackSessionManager.js:288`) — **no body**, so the caller cannot tell if the write actually happened or was silently skipped by the recency guard.
- `POST /session/local-all` -> `res.json({results: [{id, success, progressSynced}]})` (`:115-117`) — bulk variant DOES expose `progressSynced` per session. Live-confirmed: `{"results":[{"id":"...","success":true,"progressSynced":true}]}`.
- Practical implication for Campfire: if it ever calls the singular `/session/local` for a foreground sync and needs to know whether progress actually updated, it currently can't — either switch to `/session/local-all` with a single-element array, or don't rely on the write actually landing.

## Bottom line
The `PATCH /api/me/progress/:id` route is **not redundant** after a local-session sync: it's the only unconditional, server-clock-stamped progress write, and the only route that can set `isFinished` explicitly, ebook position (`ebookLocation`/`ebookProgress`), or `hideFromContinueListening`. Local-session sync is guarded by a client-supplied-timestamp comparison that a stale/misconfigured device clock can make behave unpredictably (and the singular endpoint gives no feedback when it silently no-ops).
