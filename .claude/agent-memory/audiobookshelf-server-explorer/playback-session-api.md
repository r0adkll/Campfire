---
name: playback-session-api
description: Server-created PlaybackSession API surface — /play, /public/session/:id/track/:index, /hls, sync/close semantics, lifecycle constants, and the MediaProgress double-count answer (source+live verified on v2.34.0-4-ge39e8d8c)
metadata:
  type: reference
---

Verified against `/Users/r0adkll/OpenSource/audiobookshelf` @ `v2.34.0-4-ge39e8d8c` (matches container source), live-tested on the dev server. See [[server-location-auth]] for how to start it.

## Key file locations
- `server/routers/ApiRouter.js:117-118` — `POST /api/items/:id/play`, `POST /api/items/:id/play/:episodeId`
- `server/routers/ApiRouter.js:229-238` — `/api/sessions*`, `/api/session/:id/(sync|close)`, `/api/session/local(-all)`
- `server/routers/PublicRouter.js:21` — `GET /public/session/:id/track/:index` (**no auth middleware at all**)
- `server/routers/HlsRouter.js` — `GET /hls/:stream/:file` (**no auth middleware at all**; mounted `Server.js:319`)
- `server/managers/PlaybackSessionManager.js` — all session lifecycle logic
- `server/objects/PlaybackSession.js` — in-memory session + `toJSONForClient`
- `server/objects/Stream.js` — HLS/ffmpeg transcode
- `server/objects/DeviceInfo.js` — deviceInfo shape
- `server/models/PlaybackSession.js` — DB row (no chapters/audioTracks persisted; `timeListening` is INTEGER)
- `server/models/MediaProgress.js` — **has no `timeListening` column**

## Lifecycle constants
- Stale-session cleanup: cron `30 0 * * *`, closes sessions with `updatedAt` older than **36 h** (`CronManager.js:42-50`, `PlaybackSessionManager.js:489-497`). No shorter idle timeout exists.
- `session.updatedAt` is bumped **only** by `addListeningTime` (`PlaybackSession.js:238-249`), which returns early when `timeListened` is falsy — a keepalive sync with `timeListened: 0` does NOT refresh the staleness clock.
- Session dedupe key is **(userId, deviceInfo.id)** only — NOT the item (`PlaybackSessionManager.js:315`). Starting any new `/play` closes the device's previous session, even for a different book.
- **If the client omits `deviceInfo.deviceId`, a fresh Device row + session is created every call and nothing is ever deduped** (`DeviceInfo.js:87` — `deviceId = clientDeviceInfo?.deviceId || this.id`). `getTempDeviceId()` is dead code.
- Open sessions are in-memory only (`PlaybackSessionManager.js:26`); server restart loses them, `Server.stop()` never closes them, and `removeOrphanStreams()` (`Server.js:148`) wipes all `/metadata/streams` dirs at next boot.
- DB row is only written once `timeListening > 0` (`saveSession`, `PlaybackSessionManager.js:433-442`).

## sync / close
- `POST /api/session/:id/sync` body: `{ currentTime, timeListened, duration? }`. **The key is `timeListened` (past tense) and it is a DELTA** added to `session.timeListening` (`PlaybackSessionManager.js:388`). Sending `timeListening` is silently ignored. Live-confirmed: two syncs of 60 → session.timeListening 120.
- `currentTime` is absolute and is written straight into MediaProgress with **no recency guard** (`PlaybackSessionManager.js:391-400`) — unlike `syncLocalSession`, which skips when `mediaProgress.updatedAt > session.updatedAt` (`:232`).
- `POST /api/session/:id/close` accepts the **same sync body inline**; an empty `{}` becomes `null` and just saves (`SessionController.js:178-182`). No separate sync-then-close needed.
- Emits: `user_item_progress_updated` (to session user) on sync; `user_stream_update` (admins only) + `user_session_closed` (session user) on start/close.

## MediaProgress double-count answer
`mediaProgresses` has **no timeListening column** — both `syncSession` and `syncLocalSession` funnel into `User.createUpdateMediaProgressFromPayload` → `MediaProgress.applyProgressUpdate`, which only sets `currentTime`/`progress`/`isFinished`. So progress **stomps (last write wins), never accumulates**. BUT listening-time *stats* (`ApiRouter.getUserListeningStatsHelpers`, `ApiRouter.js:530-572`) naively sum `timeListening` over **all playbackSession rows**, so a real session + a local session covering the same interval **is double-counted in stats**. Live-confirmed: 120 s server session + 300 s local session for one item → `/api/me/listening-stats` `today: 420`.

## Latent server bugs found (v2.34.0)
- `LibraryItemController.js:440` `if (!req.libraryItem.hasAudioTracks)` is dead — `LibraryItem.prototype.hasAudioTracks` is a **method** (`models/LibraryItem.js:913`) that shadows the getter at `:855`, so the expression is always a truthy function ref. `/play` on an audio-less item returns 200 with `audioTracks: []` instead of 404.
- `HlsRouter.js:84` `if (startTimeForReset)` — a reset back to time 0 returns `0`, which is falsy, so no `stream_reset` socket event fires even though ffmpeg did restart. Live-observed.
