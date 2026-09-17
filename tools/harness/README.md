# Harness

Shared Python automation for the tools that need a real Audiobookshelf server and the Android app:

| Module | What it does |
|---|---|
| `campfire_harness/config.py` | Parses the shared spec sections: `[server]`, `[sample_library]`, `[fixture]`, `[app]`, plus a device definition |
| `campfire_harness/server.py` | Runs the local Audiobookshelf checkout with a fresh data dir (`Server`), talks to its API (`AbsClient`), creates and scans the Sample Library libraries and seeds progress, sessions and playlists (`Fixture`) |
| `campfire_harness/emulator.py` | Creates pinned AVDs directly in `~/.android/avd`, boots and stops them, applies automation settings |
| `campfire_harness/app.py` | Builds and installs the app, sends the debug-only `campfire_action` intents, reads and taps the UI through uiautomator |
| `campfire_harness/proc.py` | Subprocess helpers, logging, `HarnessError` |

Users:

- `tools/screenshots/` — store screenshots; adds status-bar styling, shot steps and store output.
- `tools/testbed/` — a persistent server + emulator for agents and manual end-to-end testing.

A tool adds `tools/harness` to `sys.path` and imports `campfire_harness`. Keep store-screenshot
concerns (status bar, pinned clock, captures) out of the harness.
