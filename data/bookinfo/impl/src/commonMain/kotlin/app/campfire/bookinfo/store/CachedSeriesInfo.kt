// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.store

import app.campfire.bookinfo.api.ProviderSeries
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.serialization.Serializable

/**
 * The cached unit for one (user, provider, series). A null [series] is a cached
 * "provider has no listing for this series" — kept on a short TTL like book
 * misses, since a series may become matchable as identifiers improve.
 *
 * [matchKey] records the member identifiers that produced this row; adding a
 * book to the series (or fixing its identifiers) changes the key and refetches
 * immediately rather than waiting out the TTL.
 */
@Serializable
data class CachedSeriesInfo(
  val series: ProviderSeries? = null,
  val fetchedAt: Long,
  val matchKey: String? = null,
)

internal val SERIES_INFO_CACHE_TTL = 7.days
internal val SERIES_INFO_MISS_TTL = 1.hours

internal fun CachedSeriesInfo.isStale(nowMillis: Long, currentMatchKey: String): Boolean {
  if (matchKey != currentMatchKey) return true
  val ttl = if (series == null) SERIES_INFO_MISS_TTL else SERIES_INFO_CACHE_TTL
  return nowMillis - fetchedAt > ttl.inWholeMilliseconds
}
