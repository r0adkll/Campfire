// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.store

import app.campfire.bookinfo.api.BookCommunityInfo
import app.campfire.bookinfo.api.BookReview
import kotlin.time.Duration.Companion.hours
import kotlinx.serialization.Serializable

/**
 * The cached unit for one (user, provider, library item). A null [info] is a
 * cached "provider has no record of this book" so misses aren't re-fetched on
 * every screen open — misses use a much shorter TTL than hits, since the item
 * may be matchable later (identifier coverage on providers improves over time).
 *
 * [matchKey] records which identifiers produced this row (see
 * [app.campfire.bookinfo.api.BookMatch.cacheKey]); a row fetched with different
 * identifiers than the item currently carries is treated as stale immediately,
 * so editing ISBN/ASIN metadata on the server takes effect without waiting out
 * the TTL. Rows cached before this field existed decode as null and refetch.
 */
@Serializable
data class CachedBookInfo(
  val info: BookCommunityInfo? = null,
  val reviews: List<BookReview> = emptyList(),
  val fetchedAt: Long,
  val matchKey: String? = null,
)

internal val BOOK_INFO_CACHE_TTL = 24.hours
internal val BOOK_INFO_MISS_TTL = 1.hours

internal fun CachedBookInfo.isStale(nowMillis: Long, currentMatchKey: String): Boolean {
  if (matchKey != currentMatchKey) return true
  val ttl = if (info == null) BOOK_INFO_MISS_TTL else BOOK_INFO_CACHE_TTL
  return nowMillis - fetchedAt > ttl.inWholeMilliseconds
}
