// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.audible

import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.BookMatch
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

// Mirrors the live shape of api.audible.com/1.0/catalog/products/{asin},
// verified 2026-08-31.
private const val PRODUCT_RESPONSE = """
{
  "product": {
    "asin": "B002V0QK4C",
    "title": "Wizard's First Rule",
    "release_date": "2008-10-15",
    "content_delivery_type": "SinglePartBook",
    "rating": {
      "num_reviews": 1762,
      "overall_distribution": {
        "average_rating": 4.510169568993098,
        "display_average_rating": "4.5",
        "num_ratings": 21879,
        "num_one_star_ratings": 564,
        "num_two_star_ratings": 605,
        "num_three_star_ratings": 1373,
        "num_four_star_ratings": 3900,
        "num_five_star_ratings": 15437
      }
    },
    "product_images": {"500": "https://m.media-amazon.com/images/I/519zdwHG3GL._SL500_.jpg"}
  }
}
"""

private const val UNRATED_RESPONSE = """
{
  "product": {
    "asin": "B002V0QK4C",
    "title": "Obscure Book",
    "rating": {
      "num_reviews": 0,
      "overall_distribution": {"average_rating": 0.0, "num_ratings": 0}
    }
  }
}
"""

class AudibleBookInfoProviderTest {

  private val requests = mutableListOf<String>()

  private fun provider(
    response: String = PRODUCT_RESPONSE,
    status: HttpStatusCode = HttpStatusCode.OK,
  ): AudibleBookInfoProvider {
    val client = HttpClient(
      MockEngine { request ->
        requests += request.url.toString()
        respond(response, status)
      },
    )
    return AudibleBookInfoProvider(client)
  }

  @Test
  fun `only asin bearing matches are servable`() {
    val provider = provider()

    assertThat(provider.canServe(BookMatch.Identifiers(isbn = null, asin = "B002V0QK4C"))).isTrue()
    assertThat(provider.canServe(BookMatch.Identifiers(isbn = "9780765393043", asin = null))).isFalse()
    assertThat(provider.canServe(BookMatch.TitleAuthor("Title", "Author"))).isFalse()
  }

  @Test
  fun `the rating distribution and metadata are mapped`() = runTest {
    val result = provider().getBookInfo(BookMatch.Identifiers(isbn = null, asin = " b002v0qk4c "))

    val info = (result as BookInfoResult.Success).data
    assertThat(info.providerBookId).isEqualTo("B002V0QK4C")
    assertThat(info.providerUrl).isEqualTo("https://www.audible.com/pd/B002V0QK4C")
    assertThat(info.rating).isEqualTo(4.510169568993098)
    assertThat(info.ratingsCount).isEqualTo(21879)
    assertThat(info.ratingsDistribution).isEqualTo(
      mapOf(1 to 564, 2 to 605, 3 to 1373, 4 to 3900, 5 to 15437),
    )
    assertThat(info.reviewsCount).isEqualTo(1762)
    assertThat(info.coverUrl)
      .isEqualTo("https://m.media-amazon.com/images/I/519zdwHG3GL._SL500_.jpg")
    // The ASIN is normalized (trimmed, uppercased) into the request path.
    assertThat(requests.single().contains("/catalog/products/B002V0QK4C?")).isTrue()
  }

  @Test
  fun `reviews are decoded and mapped`() = runTest {
    val reviewsResponse = """
    {
      "customer_reviews": [
        {
          "title": "This book is a blast",
          "author_name": "Jim &quot;The Impatient&quot;",
          "body": "Great listen.&lt;br/&gt;Loved Dick Hill &amp; the story.",
          "ratings": {"overall_rating": 5, "performance_rating": 5, "story_rating": 4}
        },
        {"title": "Empty", "author_name": "Quiet", "body": "  ", "ratings": {"overall_rating": 3}}
      ]
    }
    """
    val client = HttpClient(
      MockEngine { request ->
        requests += request.url.toString()
        respond(reviewsResponse)
      },
    )

    val result = AudibleBookInfoProvider(client)
      .getReviews(BookMatch.Identifiers(isbn = null, asin = "b002v0qk4c"), limit = 5)

    val reviews = (result as BookInfoResult.Success).data
    // The blank-bodied review is dropped.
    assertThat(reviews.size).isEqualTo(1)
    val review = reviews.single()
    assertThat(review.title).isEqualTo("This book is a blast")
    assertThat(review.author).isEqualTo("Jim \"The Impatient\"")
    assertThat(review.text).isEqualTo("Great listen.<br/>Loved Dick Hill & the story.")
    assertThat(review.rating).isEqualTo(5.0)
    assertThat(review.hasSpoilers).isFalse()
    assertThat(
      requests.single().endsWith("/reviews?num_results=5&sort_by=MostHelpful"),
    ).isTrue()
  }

  @Test
  fun `entity decoding is single pass`() {
    // Double-escaped input decodes one level only: &amp;lt; becomes the
    // literal text "&lt;", never a tag.
    assertThat("5 &amp;lt; 10 &#8212; &#x27;quoted&#x27;".decodeHtmlEntities())
      .isEqualTo("5 &lt; 10 — 'quoted'")
  }

  @Test
  fun `an unrated book is a miss`() = runTest {
    val result = provider(response = UNRATED_RESPONSE)
      .getBookInfo(BookMatch.Identifiers(isbn = null, asin = "B002V0QK4C"))

    assertThat(result).isEqualTo(BookInfoResult.NotFound)
  }

  @Test
  fun `unknown asins are a miss for 404 and 400`() = runTest {
    assertThat(
      provider(response = "", status = HttpStatusCode.NotFound)
        .getBookInfo(BookMatch.Identifiers(isbn = null, asin = "B000000000")),
    ).isEqualTo(BookInfoResult.NotFound)

    assertThat(
      provider(response = "", status = HttpStatusCode.BadRequest)
        .getBookInfo(BookMatch.Identifiers(isbn = null, asin = "NOTANASIN1")),
    ).isEqualTo(BookInfoResult.NotFound)
  }

  @Test
  fun `server errors surface as failures`() = runTest {
    val result = provider(response = "", status = HttpStatusCode.InternalServerError)
      .getBookInfo(BookMatch.Identifiers(isbn = null, asin = "B002V0QK4C"))

    assertThat(result).isInstanceOf(BookInfoResult.Failure::class)
  }

  @Test
  fun `an isbn only match short circuits without a request`() = runTest {
    val result = provider().getBookInfo(BookMatch.Identifiers(isbn = "9780765393043", asin = null))

    assertThat(result).isEqualTo(BookInfoResult.NotFound)
    assertThat(requests.size).isEqualTo(0)
  }
}
