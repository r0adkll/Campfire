// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.credentials

import app.campfire.auth.api.PasswordCredentials
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * For platforms without a credential API. Android replaces it with
 * `CredentialManagerPasswordCredentials`.
 */
@ContributesBinding(AppScope::class)
@Inject
class NoOpPasswordCredentials : PasswordCredentials {
  override fun offerToSave(serverUrl: String, userName: String, password: String) = Unit
  override suspend fun find(serverUrl: String, userName: String): String? = null
}

/**
 * The id a password is saved under. The password manager only knows the app, not the server, so
 * the host keeps accounts with the same name on different servers apart — and reads well in the
 * password manager's list.
 */
internal fun passwordCredentialId(serverUrl: String, userName: String): String {
  val host = serverUrl
    .substringAfter("://")
    .substringBefore('/')
    .lowercase()
  return "$userName@$host"
}
