// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

import app.campfire.core.model.LibraryItem

/**
 * How a library series is identified against an external provider's catalog.
 * Series carry no hard identifiers in Audiobookshelf, so matching goes through
 * the series name plus the identifiers of the books the user owns in it —
 * providers should verify a candidate series by member overlap, not name alone.
 */
data class SeriesMatch(
  val seriesName: String,
  val memberMatches: List<BookMatch.Identifiers>,
) {
  /** Stable string form, used to detect when a series' identity changes. */
  val cacheKey: String
    get() = "series:$seriesName;" + memberMatches.joinToString("|") { it.cacheKey }
}

/**
 * Derives a [SeriesMatch] from a series name and the user's items in it, or
 * null when no member carries an identifier a catalog can key on.
 */
fun seriesMatch(seriesName: String, ownedItems: List<LibraryItem>): SeriesMatch? {
  val members = ownedItems
    .mapNotNull { it.bestMatch() as? BookMatch.Identifiers }
  if (members.isEmpty()) return null
  return SeriesMatch(seriesName = seriesName, memberMatches = members)
}
