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

## HLS contentUrl for TRANSCODE sessions (live-confirmed v2.34.0)
- **Stream id === PlaybackSession id, always.** `PlaybackSessionManager.js:345`: `new Stream(newPlaybackSession.id, this.StreamsPath, ...)`. `getStream(sessionId)` (`:41`) does `this.getSession(sessionId)?.stream` — there is no separate stream id anywhere.
- `contentUrl` is built in `Stream.js:100-101` — `get clientPlaylistUri() { return '/hls/${this.id}/output.m3u8' }` — **no query params, no token**. Exact live example: `POST /api/items/{id}/play` with `forceTranscode:true` → `audioTracks[0].contentUrl == "/hls/<sessionId>/output.m3u8"` where `<sessionId>` is the same as the top-level `id` in the play response. `playMethod: 2` = TRANSCODE (0=DIRECTPLAY, 1=DIRECTSTREAM/presumed, 2=TRANSCODE).
- **`/hls` has zero auth middleware** — only `/api` gets `this.auth.ifAuthNeeded(...)` (`Server.js:317-319`: `router.use('/hls', this.hlsRouter.router)` has no auth arg). Live-confirmed: GET `/hls/{sessionId}/output.m3u8` returns 200 with the m3u8 **both with and without an `Authorization` header**. Security is purely via the unguessable session UUID.
- The playlist file is guaranteed to exist by the time `/play` responds — `PlaybackSessionManager.js` `await stream.generatePlaylist()` runs before the HTTP response is sent, so there's no race where the client's first GET of `output.m3u8` beats the file write.
- **A CORRECT `{serverUrl}/hls/{sessionId}/output.m3u8` URL still 404s once the session is gone.** `getStream()` returns null for any id not in the live in-memory `sessions` array, and `HlsRouter.js:54-58` sends 404 for that. Session removal happens on:
  1. `POST /api/session/:id/close` (live-confirmed: HLS request immediately after close → 404).
  2. **A second `/play` call from the same `(userId, deviceInfo.id)`** — dedupe key noted above under Lifecycle — closes and replaces the prior session/stream, invalidating the old `contentUrl` even though the URL was constructed correctly. This is the most likely cause of a client-observed 404 if it calls `/play` more than once for the same device (e.g. a preload/prefetch call followed by the real start-playback call), or if a route base path (`ROUTER_BASE_PATH` env var, `Server.js:56,288-300`) is configured server-side but not mirrored by the client's base URL.
  3. Server restart (sessions are in-memory only, never persisted until `timeListening > 0`).
- Requesting a nonexistent/garbage session id also 404s cleanly (live-confirmed with an all-zero UUID) — same code path.

## Latent server bugs found (v2.34.0)
- `LibraryItemController.js:440` `if (!req.libraryItem.hasAudioTracks)` is dead — `LibraryItem.prototype.hasAudioTracks` is a **method** (`models/LibraryItem.js:913`) that shadows the getter at `:855`, so the expression is always a truthy function ref. `/play` on an audio-less item returns 200 with `audioTracks: []` instead of 404.
- `HlsRouter.js:84` `if (startTimeForReset)` — a reset back to time 0 returns `0`, which is falsy, so no `stream_reset` socket event fires even though ffmpeg did restart. Live-observed.
