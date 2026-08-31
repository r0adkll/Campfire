// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.audible

import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.BookMatch
import app.campfire.bookinfo.api.SeriesMatch
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

// Shapes mirror api.audible.com responses verified live 2026-08-31: a book's
// series membership, a series parent's child relationships, and a batch
// product lookup.
private const val MEMBER_BOOK_RESPONSE = """
{
  "product": {
    "asin": "B0OWNED111",
    "series": [
      {"asin": "B0UNIVERSE", "sequence": "7", "title": "The Cosmere"},
      {"asin": "B0SERIES11", "sequence": "1", "title": "The Stormlight Archive"}
    ]
  }
}
"""

private const val SERIES_PARENT_RESPONSE = """
{
  "product": {
    "asin": "B0SERIES11",
    "content_delivery_type": "BookSeries",
    "relationships": [
      {"asin": "B0OWNED111", "relationship_type": "series", "relationship_to_product": "child", "sequence": "1"},
      {"asin": "B0DRAMA111", "relationship_type": "series", "relationship_to_product": "child", "sequence": "1"},
      {"asin": "B0BOOK2222", "relationship_type": "series", "relationship_to_product": "child", "sequence": "2"},
      {"asin": "B0COMPANIO", "relationship_type": "series", "relationship_to_product": "child", "sequence": ""},
      {"asin": "B0PREORDER", "relationship_type": "series", "relationship_to_product": "child", "sequence": "6"},
      {"asin": "B0PARENT11", "relationship_type": "season", "relationship_to_product": "parent", "sequence": "9"}
    ]
  }
}
"""

private const val CHILDREN_RESPONSE = """
{
  "products": [
    {
      "asin": "B0OWNED111", "title": "The Way of Kings", "release_date": "2010-08-31",
      "rating": {"overall_distribution": {"average_rating": 4.8, "num_ratings": 100000}},
      "product_images": {"500": "https://img/1.jpg"}
    },
    {
      "asin": "B0DRAMA111", "title": "The Way of Kings (Dramatized Adaptation)", "release_date": "2016-01-01",
      "rating": {"overall_distribution": {"average_rating": 4.5, "num_ratings": 900}}
    },
    {
      "asin": "B0BOOK2222", "title": "Words of Radiance", "release_date": "2014-03-04",
      "rating": {"overall_distribution": {"average_rating": 4.9, "num_ratings": 90000}}
    },
    {
      "asin": "B0COMPANIO", "title": "Edgedancer", "release_date": "2017-10-01",
      "rating": {"overall_distribution": {"average_rating": 4.6, "num_ratings": 20000}}
    },
    {
      "asin": "B0PREORDER", "title": "Untitled Stormlight 6", "release_date": "2031-12-01",
      "rating": {"overall_distribution": {"average_rating": 0.0, "num_ratings": 0}}
    }
  ]
}
"""

class AudibleSeriesTest {

  private val requests = mutableListOf<String>()

  private fun provider(
    memberResponse: String = MEMBER_BOOK_RESPONSE,
    memberStatus: HttpStatusCode = HttpStatusCode.OK,
  ): AudibleBookInfoProvider {
    val client = HttpClient(
      MockEngine { request ->
        val url = request.url.toString()
        requests += url
        when {
          "asins=" in url -> respond(CHILDREN_RESPONSE)
          "/catalog/products/B0SERIES11" in url -> respond(SERIES_PARENT_RESPONSE)
          "/catalog/products/" in url -> respond(memberResponse, memberStatus)
          else -> respond("", HttpStatusCode.NotFound)
        }
      },
    )
    return AudibleBookInfoProvider(client)
  }

  private fun match(seriesName: String = "The Stormlight Archive") = SeriesMatch(
    seriesName = seriesName,
    memberMatches = listOf(
      BookMatch.Identifiers(isbn = null, asin = "B0OWNED111"),
      BookMatch.Identifiers(isbn = null, asin = "B0FALLBACK"),
    ),
  )

  @Test
  fun `the series resolves through membership, relationships, and one batch`() = runTest {
    val result = provider().getSeries(match())

    val series = (result as BookInfoResult.Success).data
    assertThat(series.providerSeriesId).isEqualTo("B0SERIES11")
    assertThat(series.name).isEqualTo("The Stormlight Archive")
    // membership + parent + one batch = exactly three requests.
    assertThat(requests.size).isEqualTo(3)
  }

  @Test
  fun `the membership matching the library series name wins over the universe`() = runTest {
    val result = provider().getSeries(match(seriesName = "Stormlight Archive"))

    // Normalized name match picks B0SERIES11, not the Cosmere universe listed first.
    assertThat((result as BookInfoResult.Success).data.providerSeriesId).isEqualTo("B0SERIES11")
  }

  @Test
  fun `entries keep numbered books in order and flag preorders`() = runTest {
    val result = provider().getSeries(match())

    val entries = (result as BookInfoResult.Success).data.entries
    assertThat(entries.map { it.title to it.isReleased }).isEqualTo(
      listOf(
        "The Way of Kings" to true,
        "Words of Radiance" to true,
        "Untitled Stormlight 6" to false,
      ),
    )
    // The unnumbered companion is skipped; the merge keeps owned companions.
    assertThat(entries.none { it.title == "Edgedancer" }).isTrue()
    // The duplicate sequence keeps the most-rated edition, not the adaptation.
    assertThat(entries.first().providerBookId).isEqualTo("B0OWNED111")
    assertThat(entries.first().asins).isEqualTo(listOf("B0OWNED111"))
  }

  @Test
  fun `a failed membership lookup falls through to the next owned book`() = runTest {
    var first = true
    val client = HttpClient(
      MockEngine { request ->
        val url = request.url.toString()
        requests += url
        when {
          "asins=" in url -> respond(CHILDREN_RESPONSE)
          "/catalog/products/B0SERIES11" in url -> respond(SERIES_PARENT_RESPONSE)
          first -> {
            first = false
            respond("", HttpStatusCode.NotFound)
          }
          else -> respond(MEMBER_BOOK_RESPONSE)
        }
      },
    )
    val result = AudibleBookInfoProvider(client).getSeries(match())

    assertThat(result is BookInfoResult.Success).isTrue()
    assertThat(requests.size).isEqualTo(4)
  }

  @Test
  fun `a book with no series membership is a miss`() = runTest {
    val result = provider(memberResponse = """{"product": {"asin": "B0OWNED111", "series": []}}""")
      .getSeries(match())

    assertThat(result).isEqualTo(BookInfoResult.NotFound)
  }

  @Test
  fun `isbn only members are a miss without any request`() = runTest {
    val result = provider().getSeries(
      SeriesMatch("Any", listOf(BookMatch.Identifiers(isbn = "9780765393043", asin = null))),
    )

    assertThat(result).isEqualTo(BookInfoResult.NotFound)
    assertThat(requests.size).isEqualTo(0)
  }

  @Test
  fun `sequences parse defensively`() {
    val entries = buildSeriesEntries(
      sequencesByAsin = mapOf("A1" to "1", "A2" to "1-3", "A3" to null, "A4" to "2.5"),
      products = listOf(
        AudibleProduct(asin = "A1", title = "One"),
        AudibleProduct(asin = "A2", title = "Bundle"),
        AudibleProduct(asin = "A3", title = "Unnumbered"),
        AudibleProduct(asin = "A4", title = "Novella"),
      ),
      nowIsoDate = "2026-08-31",
    )

    assertThat(entries.map { it.title }).isEqualTo(listOf("One", "Novella"))
    assertThat(entries.map { it.position }).isEqualTo(listOf(1.0, 2.5))
    // Products without a release date are treated as unreleased announcements.
    assertThat(entries.first().isReleased).isFalse()
  }
}
