# Vendored socketio-kotlin

Vendored copy of [socketio-kotlin](https://github.com/joffrey-bion/socketio-kotlin) v2.8.0
(MIT License, Copyright (c) 2023 Joffrey Bion), replacing the `org.hildan.socketio:socketio-kotlin`
artifact. Consumed by the vendored `:thirdparty:kmp-socketio` module in place of the published
artifact.

## Why

Upstream's `packetFormatRegex` matches the packet payload with a per-character alternation
(`(.|[^.])*`) that backtracks catastrophically on large payloads — ~70KB+ `user_updated` /
`item_updated` events from Audiobookshelf hang or crash the decoder.

## Local patches (marked `CAMPFIRE PATCH` in source)

- `SocketIO.kt` — `packetFormatRegex` payload match rewritten to avoid the backtracking blowup;
  see the comment on that property.

Every other file is identical to upstream — keep it that way so future re-syncs are trivial
diffs. Remove this module once upstream ships a release whose payload regex handles large packets.
