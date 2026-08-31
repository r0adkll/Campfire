// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover

import app.campfire.bookinfo.api.BookMatch
import app.campfire.bookinfo.api.SeriesMatch
import app.campfire.bookinfo.hardcover.graphql.HardcoverBookSeries
import app.campfire.bookinfo.hardcover.graphql.HardcoverCandidateBook
import app.campfire.bookinfo.hardcover.graphql.HardcoverCandidateSeriesRow
import app.campfire.bookinfo.hardcover.graphql.HardcoverEdition
import app.campfire.bookinfo.hardcover.graphql.HardcoverSeries
import app.campfire.bookinfo.hardcover.graphql.HardcoverSeriesBook
import app.campfire.bookinfo.hardcover.graphql.HardcoverSeriesSummary
import app.campfire.bookinfo.hardcover.graphql.SeriesCandidatesData
import app.campfire.bookinfo.hardcover.graphql.canonicalizeSeries
import app.campfire.bookinfo.hardcover.graphql.isReleasedBy
import app.campfire.bookinfo.hardcover.graphql.pickSeriesId
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

/**
 * Fixtures mirror the real shape of Hardcover's Stormlight Archive data
 * (verified live — see docs/research/book-metadata-providers.md): every book
 * belongs to both the saga (997, primary_books_count 10) and the wider Cosmere
 * (5497, primary_books_count 34), and the raw series listing mixes
 * translations, box sets, split audio editions, and companion novellas.
 */
private const val TODAY = "2026-08-31"

private val STORMLIGHT = HardcoverSeriesSummary(
  id = 997,
  name = "The Stormlight Archive",
  booksCount = 27,
  primaryBooksCount = 10,
  isCompleted = false,
)

private val COSMERE = HardcoverSeriesSummary(
  id = 5497,
  name = "The Cosmere",
  booksCount = 37,
  primaryBooksCount = 34,
  isCompleted = null,
)

private var nextId = 1L

private fun book(
  title: String,
  usersCount: Int = 0,
  releaseDate: String? = "2010-08-31",
  isbn13: String? = null,
  asin: String? = null,
  id: Long = nextId++,
) = HardcoverSeriesBook(
  id = id,
  slug = title.lowercase().replace(" ", "-"),
  title = title,
  releaseDate = releaseDate,
  usersCount = usersCount,
  cachedImage = null,
  editions = listOf(HardcoverEdition(isbn13 = isbn13, asin = asin)),
)

private fun row(position: Double, book: HardcoverSeriesBook, compilation: Boolean = false) =
  HardcoverBookSeries(position = position, compilation = compilation, book = book)

/** An owned book that Hardcover puts in both the saga and the universe. */
private fun candidateBook(
  stormlightPosition: Double,
  cosmerePosition: Double,
) = HardcoverCandidateBook(
  id = nextId++,
  bookSeries = listOf(
    HardcoverCandidateSeriesRow(featured = true, position = stormlightPosition, series = STORMLIGHT),
    HardcoverCandidateSeriesRow(featured = false, position = cosmerePosition, series = COSMERE),
  ),
)

private fun match(name: String) = SeriesMatch(
  seriesName = name,
  memberMatches = listOf(BookMatch.Identifiers(isbn = "9780765393043", asin = null)),
)

class CanonicalizeSeriesTest {

  @Test
  fun `the series matching the library name wins over the wider universe`() {
    val data = SeriesCandidatesData(
      books = listOf(candidateBook(1.0, 7.0), candidateBook(3.0, 21.0)),
    )

    val picked = pickSeriesId(data, match("The Stormlight Archive"))

    assertThat(picked).isNotNull()
    assertThat(picked!!.id).isEqualTo(997L)
  }

  @Test
  fun `an unrecognized library name still avoids the parent universe`() {
    // Both series are shared by every book, so the featured flag breaks the tie
    // rather than the book count — which is what previously picked The Cosmere.
    val data = SeriesCandidatesData(
      books = listOf(candidateBook(1.0, 7.0), candidateBook(3.0, 21.0)),
    )

    val picked = pickSeriesId(data, match("Stormlight (audio editions)"))

    assertThat(picked!!.id).isEqualTo(997L)
  }

  @Test
  fun `the series shared by the most books wins`() {
    val shared = HardcoverSeriesSummary(id = 42, name = "Shared", primaryBooksCount = 5)
    val onlyOne = HardcoverSeriesSummary(id = 43, name = "Fringe", primaryBooksCount = 2)
    val data = SeriesCandidatesData(
      books = listOf(
        HardcoverCandidateBook(1, listOf(HardcoverCandidateSeriesRow(false, 1.0, shared))),
        HardcoverCandidateBook(
          2,
          listOf(
            HardcoverCandidateSeriesRow(false, 2.0, shared),
            HardcoverCandidateSeriesRow(false, 1.0, onlyOne),
          ),
        ),
      ),
    )

    val picked = pickSeriesId(data, match("Unknown Name"))

    assertThat(picked!!.id).isEqualTo(42L)
  }

  @Test
  fun `no candidates yields no series`() {
    assertThat(pickSeriesId(SeriesCandidatesData(), match("Any"))).isNull()
  }

  @Test
  fun `only main books are kept`() {
    val series = HardcoverSeries(
      id = 997,
      name = "The Stormlight Archive",
      primaryBooksCount = 10,
      bookSeries = listOf(
        row(0.1, book("The Way of Kings Prime", usersCount = 203)),
        row(1.0, book("The Way of Kings", usersCount = 9505)),
        row(1.1, book("The Way of Kings, Part 1", usersCount = 437)),
        row(1.2, book("The Way of Kings, Part 2", usersCount = 67)),
        row(2.0, book("Words of Radiance", usersCount = 5971)),
        row(2.5, book("Edgedancer", usersCount = 2000)),
      ),
    )

    val result = canonicalizeSeries(series, TODAY)

    assertThat(result.entries.map { it.title })
      .isEqualTo(listOf("The Way of Kings", "Words of Radiance"))
  }

  @Test
  fun `box set compilations are dropped`() {
    val series = HardcoverSeries(
      id = 997,
      bookSeries = listOf(
        row(1.0, book("The Way of Kings", usersCount = 9505)),
        row(2.0, book("The Stormlight Archive, Books 1-4"), compilation = true),
      ),
    )

    val result = canonicalizeSeries(series, TODAY)

    assertThat(result.entries.map { it.title }).isEqualTo(listOf("The Way of Kings"))
  }

  @Test
  fun `translations lose to the most shelved edition at the same position`() {
    val series = HardcoverSeries(
      id = 997,
      bookSeries = listOf(
        row(1.0, book("Путь королей", usersCount = 0)),
        row(1.0, book("The Way of Kings", usersCount = 9505)),
        row(1.0, book("Der Weg der Könige", usersCount = 3)),
      ),
    )

    val result = canonicalizeSeries(series, TODAY)

    assertThat(result.entries.map { it.title }).isEqualTo(listOf("The Way of Kings"))
  }

  @Test
  fun `unreleased main books are flagged and kept in order`() {
    val series = HardcoverSeries(
      id = 997,
      isCompleted = false,
      bookSeries = listOf(
        row(4.0, book("Rhythm of War", usersCount = 4000, releaseDate = "2020-11-17")),
        row(4.5, book("Horneater", usersCount = 120, releaseDate = "2027-01-01")),
        row(6.0, book("Untitled Stormlight Archive #6", releaseDate = null)),
      ),
    )

    val result = canonicalizeSeries(series, TODAY)

    // Horneater sits at 4.5 — a companion, not a main entry.
    assertThat(result.entries.map { it.title to it.isReleased }).isEqualTo(
      listOf(
        "Rhythm of War" to true,
        "Untitled Stormlight Archive #6" to false,
      ),
    )
    assertThat(result.isCompleted).isEqualTo(false)
  }

  @Test
  fun `entries are sorted by position and carry identifiers`() {
    val series = HardcoverSeries(
      id = 997,
      bookSeries = listOf(
        row(2.0, book("Words of Radiance", isbn13 = "9780765326379")),
        row(1.0, book("The Way of Kings", isbn13 = "9780765393043", asin = "B003P2WO5E")),
      ),
    )

    val result = canonicalizeSeries(series, TODAY)

    assertThat(result.entries.map { it.position }).isEqualTo(listOf(1.0, 2.0))
    assertThat(result.entries.first().isbns).isEqualTo(listOf("9780765393043"))
    assertThat(result.entries.first().asins).isEqualTo(listOf("B003P2WO5E"))
  }

  @Test
  fun `rows without a position or title are skipped`() {
    val series = HardcoverSeries(
      id = 997,
      bookSeries = listOf(
        row(1.0, book("The Way of Kings")),
        HardcoverBookSeries(position = null, book = book("Positionless")),
        row(3.0, book("")),
      ),
    )

    val result = canonicalizeSeries(series, TODAY)

    assertThat(result.entries.map { it.title }).isEqualTo(listOf("The Way of Kings"))
  }

  @Test
  fun `main book count lines up with hardcover's own primary count`() {
    // Positions 1..10 are the main entries; everything else is noise.
    val series = HardcoverSeries(
      id = 997,
      primaryBooksCount = 10,
      bookSeries = buildList {
        add(row(0.1, book("The Way of Kings Prime")))
        (1..10).forEach { add(row(it.toDouble(), book("Main Book $it"))) }
        add(row(2.5, book("Edgedancer")))
        add(row(3.5, book("Dawnshard")))
        add(row(4.5, book("Horneater")))
        add(row(5.0, book("Box Set"), compilation = true))
      },
    )

    val result = canonicalizeSeries(series, TODAY)

    assertThat(result.entries.size).isEqualTo(series.primaryBooksCount)
  }

  @Test
  fun `release dates are compared against today`() {
    assertThat(isReleasedBy("2010-08-31", TODAY)).isTrue()
    assertThat(isReleasedBy("2027-01-01", TODAY)).isFalse()
    assertThat(isReleasedBy(null, TODAY)).isFalse()
    assertThat(isReleasedBy("2026-08-31T00:00:00Z", TODAY)).isTrue()
  }
}
