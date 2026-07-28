# Vendored kmp-socketio

Vendored copy of [kmp-socketio](https://github.com/HackWebRTC/kmp-socketio) v1.4.4
(MIT License, Copyright (c) 2025 HackWebRTC), replacing the `com.piasy:kmp-socketio` artifact.
Only the source sets Campfire targets are vendored (common, android, jvm, apple); js/wasm/linux/mingw
transports and upstream tests are omitted.

## Why

The published artifact's coroutine error handling crashes the app (Crashlytics issue
`4350da0dd8d479a67b4004bddebb06da`): decode exceptions escape its internal `CoroutineScope`s,
which have no exception handler, so they propagate to the platform's uncaught-exception handler.
Any single failure also cancels the scope's implicit job, silently killing the socket. Vendoring
also lets this module depend on the patched `:thirdparty:socketio-kotlin` directly instead of
masquerading its klib identity for the Kotlin/Native compiler.

## Local patches (marked `CAMPFIRE PATCH` in source)

- `engineio/transports/WebSocket.kt` — `onWsText` catches all decode exceptions (upstream only
  caught `InvalidEngineIOPacketException`, letting `InvalidSocketIOPacketException` and
  `SerializationException` crash the app); default `ioScope` is supervised.
- `socketio/IO.kt` — the library-wide work scope is supervised.
- `engineio/transports/PollingXHR.kt` — default `ioScope` is supervised.
- `global/SupervisedScope.kt` — new file: `supervisedScope()` helper
  (`SupervisorJob` + logging `CoroutineExceptionHandler`).

Everything else is identical to upstream — keep it that way so re-syncs are trivial diffs.
Remove this module (and `:thirdparty:socketio-kotlin`) once upstream releases fix the crashes.
