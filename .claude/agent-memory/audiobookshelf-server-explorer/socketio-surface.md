---
name: socketio-surface
description: Complete Socket.IO surface inventory for ABS server — connection, auth, all client→server and server→client events, broadcast scoping
metadata:
  type: reference
---

## Connection

- **Path**: `/socket.io` (default). If server is behind a `RouterBasePath`, a second server opens at `{basePath}/socket.io` for routing; legacy clients use the root path.
- **Namespace**: default namespace only (no named namespaces)
- **Transports**: both polling and WebSocket (standard socket.io 4.x defaults; CORS origin `*`)
- **Library version**: socket.io 4.7.4

## Authentication (post-connect)

1. Client connects unauthenticated — socket is registered with no user attached.
2. Client must emit `auth` with the **JWT Bearer token string** (same token from `POST /login`).
3. Server validates JWT, looks up user, emits back:
   - On success: `init` — `{userId, username, usersOnline?}` (usersOnline only for admins)
   - On failure: `auth_failed` — `{message: "Invalid token" | "Invalid user"}`
4. API keys are NOT yet supported for socket auth (TODO comment in source).

## Client → Server Events

| Event | Payload | Notes |
|-------|---------|-------|
| `auth` | `token: string` | Must be sent right after connect |
| `cancel_scan` | `libraryId: string` | Cancel in-progress library scan |
| `search_covers` | `{requestId, title, author?, provider?, podcast?}` | Stream cover art search results |
| `cancel_cover_search` | `requestId: string` | Cancel a cover search |
| `set_log_listener` | `level: number` | Start receiving server log events |
| `remove_log_listener` | (none) | Stop receiving server logs |
| `message_all_users` | `{message: string}` | Admin-only: broadcasts `admin_message` |
| `ping` | (none) | Server replies with `pong` |
| `disconnect` | (auto) | Socket.IO lifecycle |

## Server → Client Events

### Auth lifecycle (per-socket)
- `init` — `{userId, username, usersOnline?}` — sent once after successful `auth`
- `auth_failed` — `{message}` — sent on bad token/inactive user
- `pong` — empty — reply to `ping`

### Library items (filtered per user's access permissions)
- `item_added` — `libraryItem.toOldJSONExpanded()`
- `item_updated` — `libraryItem.toOldJSONExpanded()`
- `item_removed` — `{id, libraryId}` — object with IDs only
- `items_added` — `LibraryItem[]` toOldJSONExpanded — batch
- `items_updated` — `LibraryItem[]` toOldJSONExpanded — batch

### Library CRUD (all authenticated users, optionally filtered by library access)
- `library_added` — `library.toOldJSON()`
- `library_updated` — `library.toOldJSON()`
- `library_removed` — library JSON

### Author/Series/Collection (all authenticated users)
- `author_added` — `author.toOldJSON()`
- `author_updated` — `author.toOldJSONExpanded(numBooks)`
- `author_removed` — `{id, libraryId}` or `author.toOldJSON()`
- `series_added` — `series.toOldJSON()`
- `series_updated` — `series.toOldJSON()`
- `series_removed` — `{id, libraryId}`
- `collection_added` — `jsonExpanded`
- `collection_updated` — `jsonExpanded`
- `collection_removed` — `jsonExpanded`

### Per-user (clientEmitter — only sent to that user's sockets)
- `user_updated` — `user.toOldJSONForBrowser()`
- `user_item_progress_updated` — `{id: mediaProgressId, data: mediaProgress.toOldJSON()}`
- `user_session_closed` — `sessionId: string`
- `playlist_added` — `playlist jsonExpanded`
- `playlist_updated` — `playlist jsonExpanded`
- `playlist_removed` — `playlist jsonExpanded`
- `batch_quickmatch_complete` — result object
- `ereader-devices-updated` — `{ereaderDevices: [...]}` (also admin broadcast)
- `stream_open` / `stream_progress` / `stream_ready` / `stream_closed` / `stream_error` — HLS stream lifecycle (legacy streaming)

### Admin-only (adminEmitter — `isAdminOrUp` users only)
- `user_online` — `user.toJSONForPublic(sessions)`
- `user_offline` — `user.toJSONForPublic(sessions)`
- `user_added` — `user.toOldJSONForBrowser()`
- `user_removed` — user JSON
- `user_stream_update` — `user.toJSONForPublic(sessions)` — playback session changed
- `track_started` / `track_progress` / `track_finished` — encode/embed progress `{libraryItemId, ino, progress?}`
- `task_progress` — `{libraryItemId, progress}`
- `metadata_embed_queue_update` — queue state
- `share_open` / `share_closed` — `mediaItemShare.toJSONForClient()`
- `ereader-devices-updated` — `{ereaderDevices: [...]}`
- `admin_message` — `string` — triggered by `message_all_users`

### Task/background jobs (all authenticated users)
- `task_started` — `task.toJSON()`
- `task_finished` — `task.toJSON()`

### Podcast episode downloads (all authenticated users)
- `episode_download_queued` — `podcastEpisodeDownload.toJSONForClient()`
- `episode_download_started` — `podcastEpisodeDownload.toJSONForClient()`
- `episode_download_finished` — `podcastEpisodeDownload.toJSONForClient()`
- `episode_download_queue_cleared` — `libraryItemId: string`
- `episode_added` — `podcastEpisode` expanded JSON

### RSS/Feed/Notification/Misc (all authenticated users)
- `rss_feed_open` — `feed.toOldJSONMinified()`
- `rss_feed_closed` — `feed.toOldJSONMinified()`
- `notifications_updated` — `notificationSettings.toJSON()`
- `backup_applied` — (no payload)
- `stream_reset` — `{streamId, startTime}`

## Broadcast Scoping

- **`emitter(evt, data, filter?)`**: all authenticated sockets; optional filter function on user
- **`clientEmitter(userId, evt, data)`**: only sockets for that specific user
- **`adminEmitter(evt, data)`**: only users where `isAdminOrUp === true`
- **`libraryItemEmitter(evt, item)`**: only users who pass `user.checkCanAccessLibraryItem(item)`
- **`libraryItemsEmitter(evt, items[])`**: per-user filtered subset of items array

## Lifecycle Notes

- Socket connects as unauthenticated; must send `auth` to associate with a user
- A socket can re-authenticate (same or different user) without reconnecting
- On disconnect: admin sockets receive `user_offline` for the departed user; active cover searches are abandoned (no error event sent)
- No auto-rooms or channel subscriptions — all filtering is done server-side at emit time
- No scan progress events observed in source (scanner emits item changes but no progress percentage events to sockets directly)

## Key Source Files

1. `/workspaces/audiobookshelf/server/SocketAuthority.js` — connection setup, auth flow, all emitter helpers
2. `/workspaces/audiobookshelf/server/managers/PlaybackSessionManager.js` — `user_item_progress_updated`, `user_session_closed`, `user_stream_update`
3. `/workspaces/audiobookshelf/server/managers/PodcastManager.js` — all `episode_download_*` events
4. `/workspaces/audiobookshelf/server/managers/TaskManager.js` — `task_started`, `task_finished`
5. `/workspaces/audiobookshelf/server/objects/Stream.js` — HLS `stream_*` events (legacy streaming)
