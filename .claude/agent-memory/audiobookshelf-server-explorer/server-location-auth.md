---
name: Server Location & Auth
description: ABS server runs in Docker container jolly_saha; source in /workspaces/audiobookshelf; uses global.ConfigPath for DB path
type: reference
---

- Server: http://localhost:3333 (Docker dev container named `jolly_saha`)
- Source root: `/workspaces/audiobookshelf/` inside the container
- DB: `/workspaces/audiobookshelf/config/absdatabase.sqlite` (SQLite via Sequelize)
- DB init requires `global.ConfigPath` and `global.MetadataPath` to be set before `require('./server/Database')`
- Server version tested: 2.33.2
- Auth: POST /login with {username: root, password: password} → user.token (JWT); use as `Authorization: Bearer <token>`
- To read DB directly from host: `docker exec jolly_saha node -e "global.ConfigPath=...; require('./server/Database').init()..."`
- sqlite3 CLI not installed in container; use Node or Sequelize to query
