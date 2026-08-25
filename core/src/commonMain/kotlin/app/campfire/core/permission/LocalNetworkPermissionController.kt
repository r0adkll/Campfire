// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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

  /**
   * True when the platform gates local-network traffic behind a permission that has not been
   * granted yet. Unlike [requestIfNeeded], this is independent of the server address — features
   * that are inherently LAN-bound (e.g. Google Cast) need the permission even when the
   * Audiobookshelf server itself is remote.
   */
  fun isPermissionMissing(): Boolean = false

  /**
   * Explicitly prompts for the local-network permission, regardless of the server address.
   * Intended for user-initiated flows (e.g. an "allow access" action in the cast device picker),
   * so it bypasses the once-per-session throttle of [requestIfNeeded]; a permanent denial
   * resolves immediately without UI. Returns `true` when access is available afterwards.
   */
  suspend fun request(): Boolean = true
}
