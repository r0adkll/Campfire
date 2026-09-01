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
 * and their hydrated products. Sequences are strings: a blank sequence is a
 * real book Audible hasn't numbered (common for newly announced titles) and is
 * kept unnumbered, while a non-numeric one (bundles with ranges like "1-3") is
 * skipped — the merge keeps owned books the listing doesn't mention, so owned
 * bundles still render. Duplicates keep the most-rated product: by sequence
 * for numbered entries (e.g. a dramatized adaptation alongside the original),
 * by normalized title for unnumbered ones (e.g. a re-recording). Products the
 * marketplace doesn't actually serve are dropped so every entry's store link
 * resolves.
 */
internal fun buildSeriesEntries(
  sequencesByAsin: Map<String, String?>,
  products: List<AudibleProduct>,
  nowIsoDate: String,
): List<ProviderSeriesEntry> {
  val parsed = products
    .filter { it.isAvailable() }
    .mapNotNull { product ->
      val asin = product.asin ?: return@mapNotNull null
      val sequence = sequencesByAsin[asin]?.takeUnless { it.isBlank() }
      val position = sequence?.toDoubleOrNull()
      if (sequence != null && position == null) return@mapNotNull null
      val title = product.title?.takeUnless { it.isBlank() } ?: return@mapNotNull null
      Triple(position, product, title)
    }

  val numbered = parsed
    .filter { (position, _, _) -> position != null }
    .groupBy { (position, _, _) -> position }
    .map { (_, group) -> group.maxBy { (_, product, _) -> product.numRatings() } }
    .sortedBy { (position, _, _) -> position }

  val unnumbered = parsed
    .filter { (position, _, _) -> position == null }
    .groupBy { (_, _, title) -> title.normalizedTitle() }
    .map { (_, group) -> group.maxBy { (_, product, _) -> product.numRatings() } }
    .sortedBy { (_, product, _) -> product.releaseDate ?: "" }

  return (numbered + unnumbered).map { (position, product, title) ->
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
}

private fun AudibleProduct.numRatings(): Int {
  return rating?.overallDistribution?.numRatings ?: 0
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
