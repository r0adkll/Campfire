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
tools/testbed/testbed.py status                # URLs, credentials, library ids, emulator serial, captures (JSON)
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
tools/testbed/testbed.py app screencap before-delete       # → <captures>/before-delete.png
tools/testbed/testbed.py app logcat --grep LibraryItemPurger
```

For anything else, use `adb -s <serial>` with the serial from `status`.

## Captures

Every `up` starts a captures directory, `.work/captures/<started-at>/` (`--keep-captures` keeps
adding to the previous one). `status` prints its path, `down` leaves it in place, and screenshots and
recordings given a bare name land in it; give a path with a `/` to save elsewhere.

```bash
tools/testbed/testbed.py record start delete-while-away   # records until stopped, any length
tools/testbed/testbed.py record stop                      # → JSON: video, sheet, frames_dir, sheet_frames_seconds
tools/testbed/testbed.py captures                         # list this run's captures
```

`record stop` saves, in the captures directory:

- `<name>.mp4` — the full recording. `screenrecord` caps at 3 minutes, so the device records
  3-minute segments that are joined back together.
- `<name>-frames/` — every distinct screen state, one PNG each, named by its time in the video
  (`0012.40s.png`). A frame is kept when any small area of the screen changed, so a label appearing
  counts.
- `<name>-sheet.png` — up to 12 of those frames tiled in time order, left to right;
  `sheet_frames_seconds` gives their times. The quickest way to review a recording as one image.

The frames and sheet need `ffmpeg` on the `PATH`; without it you get the video only. `down`
stops and saves a recording that's still running. The captures directories are never cleaned up
automatically; delete old ones under `.work/captures/` when you no longer need them.

## Cautions

- **Never hard-delete library items** (`DELETE /api/items/<id>?hard=1`). The libraries point at the
  shared Sample Library folders, so a hard delete removes the files for the screenshot tool too. A
  plain `DELETE /api/items/<id>` removes the item from the server's database only, and a `scan`
  brings it back under a new id.
- The first boot of a newly created AVD is slow; if `up` times out waiting for Home, run it again.
- **Finish with `down`, for what you started.** An emulator left running costs gigabytes of memory
  for as long as it is up, and relaunching is cheap, so tear one down rather than keeping it warm
  for a follow-up. Two things `down` will not do for you: it only stops the server and
  `campfire-testbed`, so an AVD you booted yourself — a foldable, or one with a display cutout,
  neither of which `campfire-testbed` has — needs `adb -s <serial> emu kill`; and it stops
  `campfire-testbed` even if that was already running before you began, which `--keep-emulator`
  avoids. Note `adb devices` before you start and leave anything that was already there alone: it
  is someone's working device, not yours.
- A server left over from a crashed run: `down` stops it by the pid in `.work/state.json`. If the
  state file is gone, find it with `lsof -iTCP:13379 -sTCP:LISTEN`.
- Logs: `.work/server.log`, `.work/emulator.log`.
