package app.campfire.core.permission

/**
 * Gates access to the platform's "local network" permission (Android 16+ Local Network
 * Protection). Campfire talks to self-hosted Audiobookshelf servers that frequently live on the
 * LAN (e.g. `192.168.x.x`), and on affected platforms those connections are silently dropped
 * until the user grants access.
 *
 * The request is deferred until the user actually enters a private/LAN address, rather than
 * prompting on launch, so people connecting to a public/remote server are never asked.
 */
interface LocalNetworkPermissionController {

  /**
   * If [serverUrl] targets a private/LAN address and the local-network permission is not yet
   * granted, prompts for it (once per session) and suspends until the user responds.
   *
   * Returns `true` when access is available (granted, not needed, or the platform has no such
   * gate) and `false` when it was denied. A `false` result is non-fatal — remote servers still
   * work — so callers may proceed regardless.
   */
  suspend fun requestIfNeeded(serverUrl: String): Boolean
}
