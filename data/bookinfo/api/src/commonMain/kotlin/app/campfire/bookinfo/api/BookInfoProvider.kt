// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

import kotlinx.coroutines.flow.Flow

/**
 * A pluggable source of third-party book information (community ratings, written
 * reviews, series data, supplemental metadata).
 *
 * Implementations live in their own module (e.g. `:data:bookinfo:hardcover`) and
 * contribute themselves to the user graph with:
 *
 * ```
 * @ContributesMultibinding(UserScope::class, boundType = BookInfoProvider::class)
 * ```
 *
 * Consumers never call providers directly; they go through [BookInfoRegistry],
 * which handles provider selection, enablement, and caching.
 */
interface BookInfoProvider {
  val id: ProviderId
  val displayName: String
  val capabilities: ProviderCapabilities

  fun observeLinkState(): Flow<ProviderLinkState>

  /**
   * Whether this provider can even attempt [match] — e.g. a catalog keyed
   * solely by ISBN can't serve an ASIN-only item. The registry skips providers
   * that can't serve a match so another source gets the chance.
   */
  fun canServe(match: BookMatch): Boolean = true

  suspend fun getBookInfo(match: BookMatch): BookInfoResult<BookCommunityInfo>

  suspend fun getReviews(match: BookMatch, limit: Int = 10): BookInfoResult<List<BookReview>>
}
