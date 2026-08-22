---
status: accepted
---

# Debug-build intent hooks, not UI-driven login, put the app into screenshot state

The screenshot tool needs a logged-in, library-selected, themed app before every Shot. Driving the real login and settings UI from an instrumented test was rejected as brittle (it breaks on any copy or layout change) and slow. Instead `MainActivity` accepts extra `DeepLink`s — `Setup` (server, credentials, library, theme; runs the real `AuthRepository` flow) and `Navigate` (push a named Circuit screen) — parsed only when `BuildConfig.DEBUG`, so they are unreachable in release builds. Consequence: the hooks are a supported automation surface; changes to account setup or root navigation must keep them working, and screenshots are always taken from the `fossDebug` variant.
