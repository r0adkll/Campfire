# Agent Memory Index

- [Server Location & Auth](server-location-auth.md) — ABS runs in Docker at localhost:3333; source at /workspaces/audiobookshelf; container name changes between sessions (check `docker ps`)
- [Device Info Pipeline](device-info-pipeline.md) — Full device info flow: setData → getFromOld → DB columns; sdkVersion stored as deviceVersion STRING; extraData JSON holds manufacturer/model/osName/osVersion
- [Android sdkVersion Bug](android-sdkversion-bug.md) — Root cause: stripAllTags(Int) returns "" which || null gives null, wiping sdkVersion if sent as integer; Campfire sends string so this is NOT the active bug
- [DeviceInfo update() behavior](deviceinfo-update-behavior.md) — update() second loop nulls out fields in existing device that are missing from new payload's toJSON(); toJSON() strips null fields before comparison
- [Podcast Model & API](podcast-model.md) — Full podcast/episode shapes, serialization variants, endpoints, progress model, sort/filter keys
- [Socket.IO Surface](socketio-surface.md) — Complete inventory: connection path, post-connect auth flow, all client→server and server→client events, broadcast scoping rules
- [Auth Token Lifecycle](auth-token-lifecycle.md) — Access/refresh TTLs, /auth/refresh contract, rotation race condition, socket never re-validates JWT after initial auth, Campfire client gap
- [Book Media Shapes](book-media-shapes.md) — Exact minified/basic/expanded field lists for Book media, ebookFile shape, numMissingParts doesn't exist
