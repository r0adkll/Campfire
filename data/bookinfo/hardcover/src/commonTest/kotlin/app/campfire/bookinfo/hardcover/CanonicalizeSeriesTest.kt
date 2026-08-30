// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover

import app.campfire.bookinfo.api.BookMatch
import app.campfire.bookinfo.api.SeriesMatch
import app.campfire.bookinfo.hardcover.graphql.HardcoverBookSeries
import app.campfire.bookinfo.hardcover.graphql.HardcoverEdition
import app.campfire.bookinfo.hardcover.graphql.HardcoverSeries
import app.campfire.bookinfo.hardcover.graphql.HardcoverSeriesBook
import app.campfire.bookinfo.hardcover.graphql.canonicalizeSeries
import app.campfire.bookinfo.hardcover.graphql.isReleasedBy
import app.campfire.bookinfo.hardcover.graphql.pickSeriesCandidate
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

/**
 * Fixtures mirror the real shape of Hardcover's Stormlight Archive listing
 * (see docs/research/book-metadata-providers.md): translations and box sets
 * share positions with the canonical edition, split audio editions sit at
 * fractional positions, and unreleased books carry placeholder future years.
 */
private const val TODAY = "2026-08-31"

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

class CanonicalizeSeriesTest {

  @Test
  fun `box set compilations are dropped`() {
    val series = HardcoverSeries(
      id = 997,
      name = "The Stormlight Archive",
      bookSeries = listOf(
        row(1.0, book("The Way of Kings", usersCount = 9505)),
        row(1.0, book("The Stormlight Archive, Books 1-4"), compilation = true),
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
  fun `split part editions are dropped but companion novellas survive`() {
    val series = HardcoverSeries(
      id = 997,
      bookSeries = listOf(
        row(1.0, book("The Way of Kings", usersCount = 9505)),
        row(1.1, book("The Way of Kings, Part 1", usersCount = 437)),
        row(1.2, book("The Way of Kings, Part 2", usersCount = 67)),
        row(2.0, book("Words of Radiance", usersCount = 5971)),
        row(2.5, book("Edgedancer", usersCount = 2000)),
      ),
    )

    val result = canonicalizeSeries(series, TODAY)

    assertThat(result.entries.map { it.title })
      .isEqualTo(listOf("The Way of Kings", "Words of Radiance", "Edgedancer"))
  }

  @Test
  fun `unreleased books are flagged and kept in order`() {
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

    assertThat(result.entries.map { it.title to it.isReleased }).isEqualTo(
      listOf(
        "Rhythm of War" to true,
        "Horneater" to false,
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
  fun `release dates are compared against today`() {
    assertThat(isReleasedBy("2010-08-31", TODAY)).isTrue()
    assertThat(isReleasedBy("2027-01-01", TODAY)).isFalse()
    assertThat(isReleasedBy(null, TODAY)).isFalse()
    assertThat(isReleasedBy("2026-08-31T00:00:00Z", TODAY)).isTrue()
  }

  @Test
  fun `candidate selection prefers an exact name match`() {
    val match = SeriesMatch(
      seriesName = "The Stormlight Archive",
      memberMatches = listOf(BookMatch.Identifiers(isbn = "9780765393043", asin = null)),
    )
    val cosmere = HardcoverSeries(id = 5497, name = "The Cosmere")
    val stormlight = HardcoverSeries(id = 997, name = "Stormlight Archive")

    val picked = pickSeriesCandidate(listOf(cosmere, stormlight), match)

    assertThat(picked).isNotNull()
    assertThat(picked!!.id).isEqualTo(997L)
  }

  @Test
  fun `candidate selection falls back to member overlap`() {
    val match = SeriesMatch(
      seriesName = "Completely Different Name",
      memberMatches = listOf(
        BookMatch.Identifiers(isbn = "978-0-7653-9304-3", asin = null),
        BookMatch.Identifiers(isbn = "9780765326379", asin = null),
      ),
    )
    val unrelated = HardcoverSeries(
      id = 1,
      name = "Unrelated",
      bookSeries = listOf(row(1.0, book("Other", isbn13 = "9999999999999"))),
    )
    val stormlight = HardcoverSeries(
      id = 997,
      name = "Stormlight",
      bookSeries = listOf(
        row(1.0, book("The Way of Kings", isbn13 = "9780765393043")),
        row(2.0, book("Words of Radiance", isbn13 = "9780765326379")),
      ),
    )

    val picked = pickSeriesCandidate(listOf(unrelated, stormlight), match)

    assertThat(picked!!.id).isEqualTo(997L)
  }

  @Test
  fun `no candidates yields no series`() {
    val match = SeriesMatch("Any", listOf(BookMatch.Identifiers(isbn = "1", asin = null)))

    assertThat(pickSeriesCandidate(emptyList(), match)).isNull()
  }
}
