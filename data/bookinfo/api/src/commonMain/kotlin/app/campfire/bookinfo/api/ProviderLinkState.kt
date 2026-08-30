// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

/**
 * Whether the current user has a working account link with a provider.
 * Providers with [ProviderCapabilities.requiresAccountLink] false are always [Linked].
 */
sealed interface ProviderLinkState {
  data object NotLinked : ProviderLinkState
  data class Linked(val accountName: String?) : ProviderLinkState

  /** A credential exists but the provider rejected it; the user must re-link. */
  data object Invalid : ProviderLinkState
}
