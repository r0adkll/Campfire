---
name: Server Location & Auth
description: How to reach the local ABS dev server - host source clone, Docker container quirks, no published port, and login flow
type: reference
---

- Live server: http://localhost:3333 is only reachable from INSIDE the Docker container's network namespace — the devcontainer publishes NO ports to the host (`docker inspect <name> --format '{{json .NetworkSettings.Ports}}'` returns `{}`). Host-side `curl http://localhost:3333/...` will hang/fail (000). Always do `docker exec <container> curl ...` (or a node one-liner via `docker exec`) to hit the API, and write intermediate JSON to files INSIDE the container (e.g. `/tmp/items.json`) — `docker exec` has no access to the host's `/tmp`.
- Docker container name changes between sessions — do NOT trust a memorized name. Find it fresh each time: `docker ps -a --format "{{.Names}}\t{{.Image}}\t{{.Status}}"` and look for the `vsc-audiobookshelf-*` image. Seen names so far: `jolly_saha`, `thirsty_hopper`.
- The container is often `Exited` (stopped) — check `docker info` first; if the Docker daemon itself isn't running, `open -a Docker` and poll `docker info` until it succeeds (few seconds). Then `docker start <container>`.
- The container does NOT auto-run the ABS server on start — it just idles on a sleep loop. You must start it manually: `docker exec -d <container> sh -c "cd /workspaces/audiobookshelf && node index.js --dev > /tmp/abs.log 2>&1"`. Uses `dev.js` config: port 3333, ConfigPath=`config/`, MetadataPath=`metadata/`. Confirm startup via `docker exec <container> cat /tmp/abs.log` (look for "Listening on port :3333").
- Clean up after testing: `docker exec <container> pkill -f "node index.js"` to stop the ad hoc server (exit code 143 from pkill itself is normal/expected, not an error).
- Source root inside container: `/workspaces/audiobookshelf/`
- **A host-side clone also exists** at `/Users/r0adkll/OpenSource/audiobookshelf` — read source directly from here with the Read tool instead of `docker exec cat`, much faster. It has two remotes: `origin` (advplyr/audiobookshelf) and `fork` (r0adkll/audiobookshelf), currently on `master`. Verify freshness with `git -C /Users/r0adkll/OpenSource/audiobookshelf describe --tags` before trusting it as current (seen `v2.34.0-4-ge39e8d8c`, HEAD commit `LOCAL DEV CONTAINER` which is a harmless local devcontainer-config commit, not upstream).
- DB: `/workspaces/audiobookshelf/config/absdatabase.sqlite` (SQLite via Sequelize)
- Server version tested most recently: 2.34.0 (2026-07 session). Earlier session tested 2.33.2 under container `jolly_saha`.
- Auth: POST /login with {username: root, password: password} → `user.accessToken` (JWT); use as `Authorization: Bearer <token>`. (`user.token` is the legacy no-exp token.)
- **The devcontainer sets `ACCESS_TOKEN_EXPIRY=60` and `REFRESH_TOKEN_EXPIRY=600000` in its env** (not upstream defaults of 3600/2592000 — see [[auth-token-lifecycle]]). Access tokens die after 60 s, so re-login before every command batch. Handy helper to drop in the container:
  `printf '#!/bin/sh\ncurl -s -X POST http://localhost:3333/login -H "Content-Type: application/json" -d "{\\"username\\":\\"root\\",\\"password\\":\\"password\\"}" -o /tmp/login.json\nnode -e "require(\\"fs\\").writeFileSync(\\"/tmp/tok\\",require(\\"/tmp/login.json\\").user.accessToken)"\ncat /tmp/tok\n' > /tmp/t.sh; chmod +x /tmp/t.sh` then `TOK=$(/tmp/t.sh)`.
- Container names seen: `jolly_saha`, `thirsty_hopper`, `frosty_greider` (2026-08).
- Seed data observed: libraries "Generated Books" (book, ~2997 items, mostly single-track placeholder audio, duration 300s each), "Real Books" (book, 5 items — Starship Troopers, Dune, The Way of Kings [has PDF ebookFile], Not Till We Are Lost, Fourth Wing), "Podcasts" (podcast), plus "Play Store Books" / "Play Store Podcasts" (screenshot sample library).
- ffmpeg/ffprobe: system binaries exist at `/usr/bin` (v4.3.8) but BinaryManager downloads its own v5.1 arm64 build to `/workspaces/audiobookshelf/ffmpeg` on boot and sets `FFMPEG_PATH`. Transcoding works in this container.
