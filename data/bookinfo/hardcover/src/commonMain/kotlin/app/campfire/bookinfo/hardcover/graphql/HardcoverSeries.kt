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
 * Step 1 of series resolution: look up the books the user owns and read the
 * series Hardcover puts them in.
 *
 * This walks Hardcover's own book→series edges rather than reverse-searching
 * the catalog for series containing those books. A reverse search ranks parent
 * universes above the series you actually want (The Cosmere has more books than
 * The Stormlight Archive, and contains every Sanderson book you own), so it
 * reliably picks the wrong one.
 */
internal fun seriesCandidatesQuery(hasIsbns: Boolean, hasAsins: Boolean): String {
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
  query SeriesCandidates($variables) {
    books(where: {editions: {_or: [$predicates]}}, limit: 10) {
      id
      book_series {
        featured
        position
        series {
          id
          name
          books_count
          primary_books_count
          is_completed
        }
      }
    }
  }
  """.trimIndent()
}

/** Step 2: the chosen series' books, in reading order. */
internal val SERIES_BOOKS_QUERY = """
  query SeriesBooks(${'$'}seriesId: Int!) {
    series(where: {id: {_eq: ${'$'}seriesId}}, limit: 1) {
      id
      name
      is_completed
      books_count
      primary_books_count
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

@Serializable
internal data class SeriesCandidatesData(
  val books: List<HardcoverCandidateBook> = emptyList(),
)

@Serializable
internal data class HardcoverCandidateBook(
  val id: Long,
  @SerialName("book_series") val bookSeries: List<HardcoverCandidateSeriesRow> = emptyList(),
)

@Serializable
internal data class HardcoverCandidateSeriesRow(
  val featured: Boolean? = null,
  val position: Double? = null,
  val series: HardcoverSeriesSummary? = null,
)

@Serializable
internal data class HardcoverSeriesSummary(
  val id: Long,
  val name: String? = null,
  @SerialName("books_count") val booksCount: Int? = null,
  @SerialName("primary_books_count") val primaryBooksCount: Int? = null,
  @SerialName("is_completed") val isCompleted: Boolean? = null,
)

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
  @SerialName("primary_books_count") val primaryBooksCount: Int? = null,
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
 * Chooses which of the candidate series the user's shelf actually represents.
 *
 * A book commonly belongs to several series at once — a saga and the wider
 * universe it sits in — so preference order is:
 *
 * 1. the series whose name matches the Audiobookshelf series name
 * 2. the series shared by the most of the user's books
 * 3. the series Hardcover marks as `featured` on those books (its primary one)
 * 4. the most specific series, i.e. the fewest main books
 */
internal fun pickSeriesId(
  data: SeriesCandidatesData,
  match: SeriesMatch,
): HardcoverSeriesSummary? {
  val rows = data.books.flatMap { book ->
    book.bookSeries.mapNotNull { row -> row.series?.let { it to (row.featured == true) } }
  }
  if (rows.isEmpty()) return null

  val targetName = match.seriesName.normalizedTitle()
  rows.firstOrNull { (series, _) -> series.name?.normalizedTitle() == targetName }
    ?.let { return it.first }

  val grouped = rows.groupBy { it.first.id }
  return grouped.values
    .maxWithOrNull(
      compareBy(
        { group -> group.size },
        { group -> group.count { it.second } },
        { group -> -(group.first().first.primaryBooksCount ?: group.first().first.booksCount ?: 0) },
      ),
    )
    ?.first()
    ?.first
}

/**
 * Reduces a series' raw rows to its main books in reading order.
 *
 * Hardcover lists everything under a series: translations and box sets at the
 * same position as the canonical edition, split audio editions at fractional
 * positions, and companion novellas. `primary_books_count` is documented as
 * "main series books (excluding companions)", and those main books are exactly
 * the whole-numbered positions — so:
 *
 * 1. compilations (box sets) are dropped
 * 2. fractional positions are dropped as companions/split editions
 * 3. duplicates at one position keep the most-shelved row, so translations lose
 *    to the canonical edition
 *
 * Companions the user actually owns are unaffected: the merge keeps owned books
 * the provider listing doesn't mention.
 */
internal fun canonicalizeSeries(
  series: HardcoverSeries,
  nowIsoDate: String,
): ProviderSeries {
  data class Row(val position: Double, val book: HardcoverSeriesBook)

  val mainRows = series.bookSeries.mapNotNull { bookSeries ->
    val position = bookSeries.position ?: return@mapNotNull null
    if (bookSeries.compilation == true) return@mapNotNull null
    if (position != floor(position)) return@mapNotNull null
    val book = bookSeries.book ?: return@mapNotNull null
    if (book.title.isNullOrBlank()) return@mapNotNull null
    Row(position, book)
  }

  val entries = mainRows
    .groupBy { it.position }
    .map { (_, group) -> group.maxBy { it.book.usersCount ?: 0 } }
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
