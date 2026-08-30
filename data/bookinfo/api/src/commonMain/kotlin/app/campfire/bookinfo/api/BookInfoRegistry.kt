// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import kotlinx.coroutines.flow.Flow

/**
 * The single entry point features use to read third-party book information.
 * Selects among the installed [BookInfoProvider]s by capability, enablement, and
 * link state, and serves results through a local cache to respect provider rate
 * limits.
 */
interface BookInfoRegistry {
  fun observeProviders(): Flow<List<ProviderStatus>>

  /**
   * Community rating and reviews for [item]. By default the best available
   * provider is chosen automatically; pass [preferredProvider] to pin a
   * specific source (falls back to the automatic pick when the pinned provider
   * is disabled, unlinked, or not installed). Emits `Loaded(null)` when no
   * enabled provider can serve the item (no match identifiers, provider not
   * linked, or nothing found).
   */
  fun observeCommunityInfo(
    item: LibraryItem,
    preferredProvider: ProviderId? = null,
  ): Flow<LoadState<out CommunityInfoState?>>

  /**
   * Drops all locally cached provider data (for every provider and user).
   * Fresh data is fetched on the next read.
   */
  suspend fun clearCache()
}

data class ProviderStatus(
  val provider: BookInfoProvider,
  val enabled: Boolean,
  val linkState: ProviderLinkState,
)

/**
 * Presentation-ready community info. [providerName] and [providerUrl] must be
 * shown alongside any aggregate rating — attribution is a terms-of-service
 * requirement for some providers (e.g. Hardcover).
 *
 * @param availableSources every provider currently able to serve community
 * info, for source-switcher UI; always contains the serving provider.
 */
data class CommunityInfoState(
  val providerId: ProviderId,
  val providerName: String,
  val providerUrl: String?,
  val info: BookCommunityInfo,
  val reviews: List<BookReview>,
  val needsRelink: Boolean = false,
  val availableSources: List<CommunitySource> = emptyList(),
)

data class CommunitySource(
  val id: ProviderId,
  val name: String,
)
