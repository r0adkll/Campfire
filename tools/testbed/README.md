# Testbed

A throwaway Audiobookshelf server and emulator for exercising Campfire end to end — built for
agents (see `.claude/agents/campfire-device-tester.md`) and handy by hand. Nothing here touches a
real server or a personal AVD.

- **Server**: the local Audiobookshelf checkout on port `13379`, with a fresh data dir under
  `.work/server/`, seeded from the Sample Library (`~/Libation/StoreSampleLibrary`): an Audiobooks
  and a Podcasts library, a little listening progress, one playlist. Sign-in is `demo` / `campfire`.
- **Emulator**: the `campfire-testbed` AVD (phone, android-36 google_apis, headless by default).
- **App**: `fossDebug`, signed in through the debug-only `setup` intent.

It shares its server, emulator and app automation with the store screenshot tool through
`tools/harness/`; the prerequisites are the same (see `tools/screenshots/README.md`).

## Usage

Each command is a separate process; state in `.work/state.json` keeps the server and emulator
running between them until `down`.

```bash
tools/testbed/testbed.py up                    # server + Fixture, emulator, build + install, sign in
tools/testbed/testbed.py up --no-device        # server only
tools/testbed/testbed.py up --fresh            # restart the server from an empty data dir
tools/testbed/testbed.py up --skip-build --keep-data   # reinstall the last APK without wiping the app
tools/testbed/testbed.py status                # URLs, credentials, library ids, emulator serial (JSON)
tools/testbed/testbed.py down                  # stop everything (--keep-emulator to leave it up)
```

Server:

```bash
tools/testbed/testbed.py item "The Martian"                # → {"id", "libraryId", "title"}
tools/testbed/testbed.py api GET /api/libraries             # authenticated request, JSON out
tools/testbed/testbed.py api PATCH /api/me/progress/<id> '{"progress": 0.5}'
tools/testbed/testbed.py scan                               # rescan libraries (optionally by name) and wait
```

App:

```bash
tools/testbed/testbed.py app install [--skip-build] [--keep-data]
tools/testbed/testbed.py app sign-in [--library Podcasts]
tools/testbed/testbed.py app launch | restart | stop
tools/testbed/testbed.py app navigate library                # home, library, series, authors, collections,
                                                             # playlists, statistics, settings <Page>, library_item <id>
tools/testbed/testbed.py app open "The Martian"              # detail screen by title
tools/testbed/testbed.py app play "The Martian"              # blocks until the media session is playing
tools/testbed/testbed.py app ui --grep Martian               # on-screen labels with bounds
tools/testbed/testbed.py app tap "Download"                  # regex over text / content-description
tools/testbed/testbed.py app wait-for "Downloaded" --timeout 60000
tools/testbed/testbed.py app swipe up --times 2
tools/testbed/testbed.py app screencap /tmp/screen.png
tools/testbed/testbed.py app logcat --grep LibraryItemPurger
```

For anything else, use `adb -s <serial>` with the serial from `status`.

## Cautions

- **Never hard-delete library items** (`DELETE /api/items/<id>?hard=1`). The libraries point at the
  shared Sample Library folders, so a hard delete removes the files for the screenshot tool too. A
  plain `DELETE /api/items/<id>` removes the item from the server's database only, and a `scan`
  brings it back under a new id.
- The first boot of a newly created AVD is slow; if `up` times out waiting for Home, run it again.
- A server left over from a crashed run: `down` stops it by the pid in `.work/state.json`. If the
  state file is gone, find it with `lsof -iTCP:13379 -sTCP:LISTEN`.
- Logs: `.work/server.log`, `.work/emulator.log`.
