// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

/**
 * Implemented by [BookInfoProvider]s whose [ProviderCapabilities.requiresAccountLink]
 * is true. [verifyAndLink] must validate the credential against the provider
 * before persisting it.
 */
interface AccountLinkable {
  /** Web page where the user can create/copy the credential this provider needs. */
  val linkHelpUrl: String? get() = null

  suspend fun verifyAndLink(token: String): Result<LinkedAccount>
  suspend fun unlink()
}

data class LinkedAccount(val accountName: String?)
