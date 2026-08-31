// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.audible

import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.SeriesMatch

/**
 * Picks which of a book's series memberships the library series represents:
 * the one matching the Audiobookshelf series name when it does, otherwise the
 * first (books are rarely in more than one Audible series).
 */
internal fun pickMembership(
  memberships: List<AudibleSeriesMembership>,
  match: SeriesMatch,
): AudibleSeriesMembership? {
  val candidates = memberships.filter { !it.asin.isNullOrBlank() }
  if (candidates.isEmpty()) return null
  val targetName = match.seriesName.normalizedTitle()
  return candidates.firstOrNull { it.title?.normalizedTitle() == targetName }
    ?: candidates.first()
}

/**
 * Builds the canonical series listing from the series parent's child sequences
 * and their hydrated products. Sequences are strings; unnumbered children
 * (companions, bundles with ranges like "1-3") are skipped — the merge keeps
 * owned books the listing doesn't mention, so owned companions still render.
 * Duplicate sequences (e.g. a dramatized adaptation alongside the original)
 * keep the most-rated product.
 */
internal fun buildSeriesEntries(
  sequencesByAsin: Map<String, String?>,
  products: List<AudibleProduct>,
  nowIsoDate: String,
): List<ProviderSeriesEntry> {
  return products
    .mapNotNull { product ->
      val asin = product.asin ?: return@mapNotNull null
      val position = sequencesByAsin[asin]?.toDoubleOrNull() ?: return@mapNotNull null
      val title = product.title?.takeUnless { it.isBlank() } ?: return@mapNotNull null
      Triple(position, product, title)
    }
    .groupBy { (position, _, _) -> position }
    .map { (_, group) ->
      group.maxBy { (_, product, _) ->
        product.rating?.overallDistribution?.numRatings ?: 0
      }
    }
    .map { (position, product, title) ->
      val asin = product.asin!!
      ProviderSeriesEntry(
        providerBookId = asin,
        position = position,
        title = title,
        releaseDate = product.releaseDate,
        isReleased = isReleasedBy(product.releaseDate, nowIsoDate),
        providerUrl = audibleProductUrl(asin),
        coverUrl = product.coverUrl(),
        isbns = emptyList(),
        asins = listOf(asin),
      )
    }
    .sortedBy { it.position }
}

/** ISO dates compare lexicographically; a missing date means announced-only. */
internal fun isReleasedBy(releaseDate: String?, nowIsoDate: String): Boolean {
  val date = releaseDate?.take(10)?.takeUnless { it.isBlank() } ?: return false
  return date <= nowIsoDate
}

internal fun String.normalizedTitle(): String {
  return lowercase()
    .filter { it.isLetterOrDigit() }
    .removePrefix("the")
}
