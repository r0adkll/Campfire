---
name: auth-token-lifecycle
description: JWT access/refresh token TTLs, /auth/refresh contract, rotation semantics, and socket auth expiry behavior (source-verified on v2.34.0-4-ge39e8d8c)
metadata:
  type: reference
---

## Source location & version
- Local checkout: `/Users/r0adkll/OpenSource/audiobookshelf` (not a docker mount this time — differs from [[server-location-auth]] which references `/workspaces/audiobookshelf` in a container). Tested against source only; no running server this session.
- Version at time of analysis: `v2.34.0-4-ge39e8d8c` (2026-05-08), newer than the 2.33.2 noted in [[server-location-auth]].

## Default TTLs (`server/auth/TokenManager.js:14-17`, constructor)
- Access token: 1 hour (3600s), env override `ACCESS_TOKEN_EXPIRY` (seconds)
- Refresh token: 30 days (2592000s), env override `REFRESH_TOKEN_EXPIRY` (seconds)
- Legacy `generateAccessToken` (deprecated, static+instance, lines 92-106) issues a JWT with **no** `exp` claim at all — passport flags `user.isOldToken = true` for these (`jwtAuthCheck`, lines 263-270). Not used by `createTokensAndSession`/login flow; likely dead code path kept for back-compat.

## Socket auth: expired/invalid token behavior (`server/SocketAuthority.js:249-306`)
- Client emits `auth` with token string → `authenticateSocket(socket, token)`.
- `TokenManager.validateAccessToken(token)` (`TokenManager.js:76-82`) is a **static** `jwt.verify` wrapped in try/catch returning `null` on any failure (expired, bad signature, malformed) — no distinction in error type is surfaced to the socket layer.
- Failure paths, **none of which call `socket.disconnect()`**:
  - No/invalid token data → `auth_failed` `{message: 'Invalid token'}` (line 258)
  - Valid token but user not found → `auth_failed` `{message: 'Invalid token'}` (line 266)
  - User found but `!isActive` → `auth_failed` `{message: 'Invalid user'}` (line 270)
- **Critical**: if a socket was already authenticated (`client.user` set from a prior successful `auth`) and then emits `auth` again with an expired/bad token, `auth_failed` fires but `client.user` is never cleared — the socket keeps receiving all authenticated broadcasts it already qualified for. A failed re-auth attempt does not downgrade or revoke the existing session.
- **No periodic re-validation exists at all.** Grepped `SocketAuthority.js` for `setInterval`/`setTimeout`/expiry checks — none found. Once `client.user` is set (line 291), the server never re-checks the JWT's `exp` for that live connection. A socket that authenticated once will keep receiving broadcasts indefinitely past its access token's expiry, until the underlying transport disconnects (network drop, app background/reconnect, explicit logout, server restart). **There is no server-initiated re-challenge or kick.**
- Implication for client re-auth strategy: the client must proactively re-emit `auth` with a fresh token after HTTP-side token refresh; waiting for the server to complain is not viable since it never will while the socket stays connected.

## `POST /auth/refresh` contract (`server/Auth.js:329-357`, guarded by `authRateLimiter`)
- Rate limit default: 40 attempts / 10 min window per IP (`server/utils/RateLimiterFactory.js:9-10`), overridable via `RATE_LIMIT_AUTH_MAX` / `RATE_LIMIT_AUTH_WINDOW` env vars, or disabled with `RATE_LIMIT_AUTH_MAX=0`.
- Token source resolution (lines 330-338):
  1. Default: `req.cookies.refresh_token`
  2. If header `x-refresh-token` present, it **overrides** the cookie value AND sets `shouldReturnRefreshToken = true` (mobile/API client path)
- No token found in either place → `401 {error: 'No refresh token provided'}` (line 342)
- Delegates to `TokenManager.handleRefreshToken` (`TokenManager.js:285-360`):
  - `jwt.verify(refreshToken, secret)` — `TokenExpiredError` → destroys the matching session row, returns `{error: 'Refresh token expired'}`; `JsonWebTokenError` (bad sig/malformed) → `{error: 'Invalid refresh token'}`; any other → same generic message. All surfaced by the route as **HTTP 401** regardless of specific reason (`Auth.js:348-350`).
  - `decoded.type !== 'refresh'` → `{error: 'Invalid token type'}`
  - `Session.findOne({ where: { refreshToken } })` (exact string match) not found → `{error: 'Invalid refresh token'}`
  - Session found but DB-level `session.expiresAt < now` (separate from the JWT's own `exp` claim — both must be valid) → destroys session, `{error: 'Refresh token expired'}`
  - User lookup by `decoded.userId`; missing/inactive → `{error: 'User not found or inactive'}`
  - Success → `rotateTokensForSession` (`TokenManager.js:188-208`)
- **200 response body** = `getUserLoginResponsePayload(user)` (`Auth.js:96-105`) plus:
  ```json
  {
    "user": { "...user.toOldJSONForBrowser() fields...", "accessToken": "<new JWT>", "refreshToken": "<new JWT>|null" },
    "userDefaultLibraryId": "string",
    "serverSettings": { "...": "..." },
    "ereaderDevices": [ "..." ],
    "Source": "string"
  }
  ```
  `user.refreshToken` is only populated in the JSON body when the request used the `x-refresh-token` header (mobile/API clients); cookie-based (browser) clients get `null` there and must rely on the `Set-Cookie` response header instead.
- Cookie set on **every** successful refresh regardless of header vs cookie flow (`TokenManager.js:58-66` `setRefreshTokenCookie`, called unconditionally inside `rotateTokensForSession`): `refresh_token`, `httpOnly`, `secure` (if `req.secure` or `x-forwarded-proto: https`), `sameSite: lax`, `maxAge: RefreshTokenExpiry*1000`, `path: /`.

## Refresh token rotation & concurrency (`TokenManager.js:188-208`, `models/Session.js`)
- Rotation mutates the **same session row** — `session.refreshToken` and `session.expiresAt` are overwritten in place, not a new row inserted. The old token string no longer matches any session afterward, so a stale/reused refresh token gets `Invalid refresh token` (401) on next use — effectively single-use by side effect, not by explicit design.
- **No transaction or row locking** around the `findOne` → mutate → `save()` sequence. `Session.refreshToken` column has **no unique constraint** (`models/Session.js:62-65`, plain `STRING, allowNull:false`).
- Race condition: two concurrent `/auth/refresh` calls presenting the *same* still-valid old refresh token can both pass `jwt.verify` and both `findOne` before either write lands. Both proceed to generate distinct new token pairs and both call `session.save()` — last write wins in the DB. Both callers receive a 200 with valid new access tokens (stateless, no DB check needed for those), but whichever refresh token was NOT the final DB value becomes orphaned — that caller's next refresh attempt will fail with `Invalid refresh token`, forcing a full re-login. Worth a client-side mutex/single-flight around refresh to avoid self-inflicted lockout, especially since Campfire's Ktor `bearer { refreshTokens {} }` block already provides single-flight semantics per HttpClient instance — but a separate refresh triggered by the socket layer racing the HTTP layer's refresh would NOT be covered by that.

## Client-side gap found in Campfire (as of 2026-07-11, branch `dh/fixes-and-polish`)
- `infra/socket/impl/.../DefaultSocketManager.kt` only emits `auth` on `EVENT_CONNECT` (fresh connection) or after its own `auth_failed` retry loop (capped `MAX_AUTH_RETRIES = 3`, then permanently gives up until manual `retryConnection()`).
- `data/network/impl/.../HttpClientModule.kt` Ktor `Auth` plugin refreshes tokens lazily on HTTP 401 and stores them via `AccountManager.updateToken` — but nothing observes that update to trigger a fresh socket `auth` emit.
- Net effect: since the server (per above) never re-challenges an already-authenticated socket and never disconnects it on a failed re-auth, a long-lived socket connection can silently keep working past access-token expiry (broadcasts keep flowing) — but if it ever needs to re-authenticate (reconnect after network blip, app resume, etc.) while relying on a stale token from `AccountManager`, it hits `auth_failed` up to 3 times and then dies permanently, even though a valid token exists (the HTTP layer refreshed it independently, but the socket never learned about it before/while retrying).
