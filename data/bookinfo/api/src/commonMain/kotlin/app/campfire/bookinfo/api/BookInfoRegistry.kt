// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

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
   * is disabled, unlinked, or not installed).
   *
   * Emits null only when nothing could ever serve the item — no identifiers,
   * or no enabled provider can match it. Otherwise the emitted state always
   * carries the serving provider and switchable sources, with the fetch phase
   * expressed in [CommunityInfoState.content] — so a section rendered from
   * this state stays put across loads and source switches.
   */
  fun observeCommunityInfo(
    item: LibraryItem,
    preferredProvider: ProviderId? = null,
  ): Flow<CommunityInfoState?>

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
 * Presentation-ready community info from one serving provider. The provider
 * identity doubles as the required attribution for aggregate data (a
 * terms-of-service requirement for some providers, e.g. Hardcover) and must
 * render wherever [CommunityContent.Available] data does.
 *
 * @param availableSources every provider currently able to serve the item,
 * for source-switcher UI; always contains the serving provider.
 * @param reviewsLinkProviderName when the serving provider has no review text
 * but linking another provider (e.g. Hardcover) would add reviews, the name of
 * that provider — for a "connect for reviews" affordance.
 */
data class CommunityInfoState(
  val providerId: ProviderId,
  val providerName: String,
  val availableSources: List<CommunitySource> = emptyList(),
  val needsRelink: Boolean = false,
  val reviewsLinkProviderName: String? = null,
  val content: CommunityContent = CommunityContent.Loading,
)

sealed interface CommunityContent {
  /** The serving provider is being fetched with nothing cached yet. */
  data object Loading : CommunityContent

  /** The serving provider has nothing for this book. */
  data object Unavailable : CommunityContent

  data class Available(
    val info: BookCommunityInfo,
    val reviews: List<BookReview>,
  ) : CommunityContent
}

data class CommunitySource(
  val id: ProviderId,
  val name: String,
)
