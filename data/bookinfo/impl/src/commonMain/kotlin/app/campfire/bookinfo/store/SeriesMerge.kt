// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.store

import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeries
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.core.model.LibraryItem

/**
 * Merges the books a user owns in a series with a provider's canonical listing.
 *
 * Owned books are matched to provider entries by identifier first (ISBN/ASIN,
 * punctuation-insensitive) and normalized title second, so an owned book never
 * also shows up as "missing". Provider entries with no owned match become
 * [SeriesEntry.Missing] when released and [SeriesEntry.Upcoming] when not.
 * Owned books the provider doesn't list are kept — the user's library is the
 * source of truth for what they have — ordered by their Audiobookshelf series
 * sequence, falling back to the end of the list.
 */
internal fun mergeSeriesEntries(
  ownedItems: List<LibraryItem>,
  series: ProviderSeries?,
  providerId: ProviderId,
): List<SeriesEntry> {
  if (series == null) {
    return ownedItems
      .sortedBy { it.seriesSequenceOrNull() ?: Double.MAX_VALUE }
      .map { SeriesEntry.Owned(it) }
  }

  val remaining = ownedItems.toMutableList()
  val positioned = mutableListOf<Pair<Double, SeriesEntry>>()

  for (entry in series.entries) {
    val ownedIndex = remaining.indexOfFirst { it.matches(entry.isbns, entry.asins, entry.title) }
    if (ownedIndex >= 0) {
      val owned = remaining.removeAt(ownedIndex)
      positioned += entry.position to SeriesEntry.Owned(owned)
    } else if (entry.isReleased) {
      positioned += entry.position to SeriesEntry.Missing(entry, providerId)
    } else {
      positioned += entry.position to SeriesEntry.Upcoming(entry, providerId)
    }
  }

  // Owned books the provider doesn't list still belong to the user's series.
  for (owned in remaining) {
    positioned += (owned.seriesSequenceOrNull() ?: Double.MAX_VALUE) to SeriesEntry.Owned(owned)
  }

  return positioned
    .sortedBy { it.first }
    .map { it.second }
}

private fun LibraryItem.matches(
  isbns: List<String>,
  asins: List<String>,
  title: String,
): Boolean {
  val metadata = media.metadata
  val ownedIsbn = metadata.ISBN?.normalizedIdentifier()
  if (ownedIsbn != null && isbns.any { it.normalizedIdentifier() == ownedIsbn }) return true
  val ownedAsin = metadata.ASIN?.normalizedIdentifier()
  if (ownedAsin != null && asins.any { it.normalizedIdentifier() == ownedAsin }) return true
  val ownedTitle = metadata.title?.normalizedTitle()
  return ownedTitle != null && ownedTitle.isNotEmpty() && ownedTitle == title.normalizedTitle()
}

private fun LibraryItem.seriesSequenceOrNull(): Double? {
  return media.metadata.seriesSequence?.sequence
}

private fun String.normalizedIdentifier(): String? {
  return filter { it.isLetterOrDigit() }.uppercase().takeUnless { it.isEmpty() }
}

private fun String.normalizedTitle(): String {
  return lowercase()
    .filter { it.isLetterOrDigit() }
    .removePrefix("the")
}
