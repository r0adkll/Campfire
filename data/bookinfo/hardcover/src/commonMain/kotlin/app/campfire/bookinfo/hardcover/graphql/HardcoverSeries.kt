// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.graphql

import app.campfire.bookinfo.api.ProviderSeries
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.SeriesMatch
import kotlin.math.floor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Finds candidate series by member books rather than by name — Audiobookshelf
 * series names don't reliably match Hardcover's, but the user's books do.
 * GraphQL requires every declared variable to be used, so declarations and
 * predicates are built together.
 */
internal fun seriesByMembersQuery(hasIsbns: Boolean, hasAsins: Boolean): String {
  require(hasIsbns || hasAsins) { "At least one identifier list is required" }
  val variables = buildList {
    if (hasIsbns) add("${'$'}isbns: [String!]!")
    if (hasAsins) add("${'$'}asins: [String!]!")
  }.joinToString(", ")
  val predicates = buildList {
    if (hasIsbns) {
      add("{isbn_13: {_in: ${'$'}isbns}}")
      add("{isbn_10: {_in: ${'$'}isbns}}")
    }
    if (hasAsins) {
      add("{asin: {_in: ${'$'}asins}}")
    }
  }.joinToString(", ")
  return """
  query SeriesByMembers($variables) {
    series(
      where: {book_series: {book: {editions: {_or: [$predicates]}}}}
      order_by: {books_count: desc}
      limit: 5
    ) {
      id
      name
      is_completed
      books_count
      book_series(order_by: {position: asc}) {
        position
        compilation
        book {
          id
          slug
          title
          release_date
          users_count
          cached_image
          editions(limit: 25) {
            isbn_10
            isbn_13
            asin
          }
        }
      }
    }
  }
  """.trimIndent()
}

@Serializable
internal data class SeriesData(
  val series: List<HardcoverSeries> = emptyList(),
)

@Serializable
internal data class HardcoverSeries(
  val id: Long,
  val name: String? = null,
  @SerialName("is_completed") val isCompleted: Boolean? = null,
  @SerialName("books_count") val booksCount: Int? = null,
  @SerialName("book_series") val bookSeries: List<HardcoverBookSeries> = emptyList(),
)

@Serializable
internal data class HardcoverBookSeries(
  val position: Double? = null,
  val compilation: Boolean? = null,
  val book: HardcoverSeriesBook? = null,
)

@Serializable
internal data class HardcoverSeriesBook(
  val id: Long,
  val slug: String? = null,
  val title: String? = null,
  @SerialName("release_date") val releaseDate: String? = null,
  @SerialName("users_count") val usersCount: Int? = null,
  @SerialName("cached_image") val cachedImage: JsonElement? = null,
  val editions: List<HardcoverEdition> = emptyList(),
)

@Serializable
internal data class HardcoverEdition(
  @SerialName("isbn_10") val isbn10: String? = null,
  @SerialName("isbn_13") val isbn13: String? = null,
  val asin: String? = null,
)

/**
 * Picks the series the user's books actually belong to: exact normalized name
 * match first (a book can sit in several series, e.g. a saga and its parent
 * universe), then the candidate overlapping the most member identifiers.
 */
internal fun pickSeriesCandidate(
  candidates: List<HardcoverSeries>,
  match: SeriesMatch,
): HardcoverSeries? {
  if (candidates.isEmpty()) return null
  val targetName = match.seriesName.normalizedTitle()
  candidates.firstOrNull { it.name?.normalizedTitle() == targetName }?.let { return it }

  val memberIds = buildSet {
    match.memberMatches.forEach { member ->
      member.isbn?.normalizedIdentifier()?.let(::add)
      member.asin?.normalizedIdentifier()?.let(::add)
    }
  }
  return candidates.maxByOrNull { candidate -> candidate.memberOverlap(memberIds) }
}

private fun HardcoverSeries.memberOverlap(memberIds: Set<String>): Int {
  return bookSeries.count { row ->
    row.book?.editions.orEmpty().any { edition ->
      edition.isbn13?.normalizedIdentifier() in memberIds ||
        edition.isbn10?.normalizedIdentifier() in memberIds ||
        edition.asin?.normalizedIdentifier() in memberIds
    }
  }
}

/**
 * Reduces Hardcover's raw series rows — which mix translations, box sets, and
 * split editions at overlapping positions — to one canonical entry per
 * position:
 *
 * 1. compilations (box sets) are dropped
 * 2. duplicate positions keep the most-shelved row (translations lose to the
 *    canonical edition on `users_count`)
 * 3. fractional positions that just re-title their base book (split parts,
 *    dramatized adaptations) are dropped; genuine companion novellas survive
 */
internal fun canonicalizeSeries(
  series: HardcoverSeries,
  nowIsoDate: String,
): ProviderSeries {
  data class Row(val position: Double, val book: HardcoverSeriesBook)

  val rows = series.bookSeries.mapNotNull { bookSeries ->
    val position = bookSeries.position ?: return@mapNotNull null
    if (bookSeries.compilation == true) return@mapNotNull null
    val book = bookSeries.book ?: return@mapNotNull null
    if (book.title.isNullOrBlank()) return@mapNotNull null
    Row(position, book)
  }

  val byPosition = rows
    .groupBy { it.position }
    .mapValues { (_, group) -> group.maxBy { it.book.usersCount ?: 0 } }

  val entries = byPosition.values
    .filter { row ->
      val isWholePosition = row.position == floor(row.position)
      if (isWholePosition) return@filter true
      val baseTitle = byPosition[floor(row.position)]?.book?.title?.normalizedTitle()
      baseTitle == null || !row.book.title!!.normalizedTitle().startsWith(baseTitle)
    }
    .map { row ->
      val book = row.book
      ProviderSeriesEntry(
        providerBookId = book.id.toString(),
        position = row.position,
        title = book.title!!,
        releaseDate = book.releaseDate,
        isReleased = isReleasedBy(book.releaseDate, nowIsoDate),
        providerUrl = book.slug?.let { "https://hardcover.app/books/$it" },
        coverUrl = parseCoverUrl(book.cachedImage),
        isbns = book.editions
          .mapNotNull { it.isbn13?.normalizedIdentifier() ?: it.isbn10?.normalizedIdentifier() }
          .distinct(),
        asins = book.editions
          .mapNotNull { it.asin?.normalizedIdentifier() }
          .distinct(),
      )
    }
    .sortedBy { it.position }

  return ProviderSeries(
    providerSeriesId = series.id.toString(),
    name = series.name.orEmpty(),
    isCompleted = series.isCompleted,
    entries = entries,
  )
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

internal fun String.normalizedIdentifier(): String? {
  return filter { it.isLetterOrDigit() }.uppercase().takeUnless { it.isEmpty() }
}
