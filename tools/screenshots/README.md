# Store screenshots

Reproducible Play / F-Droid screenshots for phone and tablet. One command boots a fresh local
Audiobookshelf server seeded with the Sample Library, boots a pinned emulator, signs the app in,
walks each Shot in `shots.toml`, and writes the PNGs into Store Metadata (`fastlane/metadata/android`).

Vocabulary: a **Shot** is one named screenshot (screen to reach, device classes, locale, theme);
the **Shot Spec** is `shots.toml`; a **Device Class** is a store screenshot category (phone,
seven-inch, ten-inch) backed by one pinned emulator; the **Fixture** is the known server state a run
starts from (Sample Library + fresh scan + seeded progress/sessions); **Store Metadata** is the
`fastlane/` tree.

## Prerequisites

- macOS with Python 3.11+, Node 20+, `git`, and the Android SDK (`ANDROID_HOME` or `~/Library/Android/sdk`)
  with `platform-tools`, `emulator`, and the system image named in `shots.toml`
  (`system-images;android-36;google_apis;arm64-v8a`).
- The Sample Library at `~/Libation/StoreSampleLibrary` (or change `[sample_library].path`).
- An Audiobookshelf checkout at `~/OpenSource/audiobookshelf`. If the path is missing the tool clones
  it at the pinned tag; if it exists on another ref the tool warns and continues.

## Usage

```bash
tools/screenshots/run.py --class phone            # every enabled phone shot → phoneScreenshots/
tools/screenshots/run.py --class seven            # sevenInchScreenshots/
tools/screenshots/run.py --class ten              # tenInchScreenshots/
tools/screenshots/run.py --class phone --shots Home,Player   # only those (others left untouched)
tools/screenshots/run.py --class phone --out /tmp/preview    # don't touch Store Metadata
```

Flags: `--locale de-DE` (switches the emulator locale), `--crop-9-16` (opt-in, see "Design decisions"),
`--cold`, `--headless`, `--skip-build`, `--keep-server`, `--keep-emulator`, `--regenerate`, `--server-only`
(start the server + Fixture and wait, for poking at the app by hand).

A run with no `--shots` filter **replaces** the class directory; a filtered run overwrites only the
named shots. Files are numbered by spec order (`01_Home.png`, …). Commit the result.

## What a run does

1. **Server** — starts `node index.js` from the checkout on `[server].port` with a throwaway data dir
   under `tools/screenshots/.work/server/`, creates the root user, creates one library per
   `[[sample_library.libraries]]`, scans, then seeds `[[fixture.sessions]]` (backdated listening
   sessions for the statistics screen), `[[fixture.playlists]]`, and `[[fixture.progress]]`
   (Continue Listening / Listen Again) — all by title. With `[fixture] match_authors = true` every
   author is quick-matched against Audible through the server so the Authors screen has real photos
   (needs internet; unmatched authors keep the placeholder).
   The server is always stopped afterwards unless `--keep-server`.
2. **Emulator** — stops any *other* `campfire-shots-*` emulator (personal AVDs are never touched),
   creates `campfire-shots-<class>` from `[classes.<class>]` if missing (written directly
   to `~/.android/avd`, no `avdmanager` needed), reuses it if already running, disables window
   animations, and shapes the status bar: clock pinned to 12:00 (root `date`, re-pinned before every
   capture), full battery and full cellular signal via the emulator console, SystemUI restarted so no
   stale icons remain. SystemUI *demo mode* is deliberately not used — on the pinned android-36
   image it renders broken glyphs. The soft keyboard is suppressed by disabling the keyboard IMEs
   and selecting the (invisible) voice IME, so `type` steps never show Gboard.
3. **App** — builds `[app].variant` with `-Pcampfire_no_test_credentials=true` (so a developer's
   login prefill from `~/.gradle/gradle.properties` never ends up in a shot — prefer a fresh build over
   `--skip-build` after changing those properties), installs it, clears its data, then sends the debug-only
   `setup` intent (server URL, credentials, library, theme) and waits until a signed-in Home is on
   screen (dismissing an ANR dialog or re-sending setup if needed). Release builds ignore these intents.
4. **Shots** — runs each shot's steps, waits `settle_ms`, captures with `screencap`.
5. **Output** — copies into Store Metadata (or `--out`). The emulator and server are then stopped
   (`--keep-emulator` / `--keep-server` to leave them up, e.g. while tuning a shot).

## Editing the Shot Spec

```toml
[[shot]]
name = "Detail2"
classes = ["phone"]                 # which Device Classes get this shot
steps = [
  { navigate = "library_item", title = "Dungeon Crawler Carl" },
  { swipe = "up", times = 2 },
]
# optional: enabled = false, library = "Podcasts", theme_mode = "dark", theme = "Forest", settle_ms = 3000
```

Step kinds: `welcome = true` (signed-out Welcome screen), `navigate` (`home`, `library`, `series`, `authors`, `collections`, `playlists`,
`statistics`, `theme_picker`, `settings` + `arg = "<Page>"`, `library_item` + `title = "…"`),
`play = "<title>"`, `expand_player = true`, `tap = "<regex over text / content-description>"`,
`type = "…"`, `swipe = "up"|"down"`, `wait = <ms>`, `back = true`.

Titles are resolved against the running server, so anything in the Sample Library works. Playback
shots are captured *while playing* — the progress position will differ run to run by design.

## Design decisions

- **Native resolution, not Play's 9:16.** Google Play recommends 9:16 (1080×1920) phone screenshots and
  gates some promotion placements on it, but a 9:16 crop would cut the bottom of every shot — exactly
  where the mini player and navigation live — and the same files feed F-Droid/IzzyOnDroid, which have
  no ratio requirement. Files are committed at the emulator's native size; `--crop-9-16` exists for
  the day promotion eligibility matters more.
- **Debug intent hooks, not UI-driven login.** Driving the real login/settings UI from an instrumented
  test is brittle (breaks on any copy or layout change) and slow. `MainActivity` accepts extra
  `DeepLink`s (`setup`, `navigate`, `play`, `expand_player`, `stop_playback`) only when
  `BuildConfig.DEBUG`, so they are unreachable in release builds. Consequence: these hooks are a
  supported automation surface — changes to account setup or root navigation must keep them working,
  and screenshots are always taken from `fossDebug`.

## Troubleshooting

- Logs: `tools/screenshots/.work/server.log`, `.work/emulator-<class>.log`; raw captures in `.work/captures/`.
- "Something is already listening on port" — a dev ABS container is using it; change `[server].port`.
- `tap` can't find a node — run with `--keep-server`, then `adb shell uiautomator dump` to see labels.
- A "Campfire isn't responding" dialog in a capture — the animator duration scale must stay at 1 on
  the emulator (`settings get global animator_duration_scale`); at 0 the app's infinite animations ANR.
- Local-network permission prompt on Android 16+ — the tool `pm grant`s it; if a prompt still shows, the
  AVD was created from a Play-store image (use the pinned `google_apis` image).
