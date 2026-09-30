// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.api

/**
 * The passwords the user keeps in their password manager, through the platform's credential API
 * (Credential Manager on Android 14+). Where that isn't available, nothing is saved or found, so
 * callers don't need to check.
 */
interface PasswordCredentials {

  /**
   * Offer to save [password] for [userName] on [serverUrl]. Returns right away; the platform may
   * show its own prompt, which outlives the caller.
   */
  fun offerToSave(serverUrl: String, userName: String, password: String)

  /**
   * Ask the user for the saved password of [userName] on [serverUrl], or null when there's none or
   * they decline.
   */
  suspend fun find(serverUrl: String, userName: String): String?
}
