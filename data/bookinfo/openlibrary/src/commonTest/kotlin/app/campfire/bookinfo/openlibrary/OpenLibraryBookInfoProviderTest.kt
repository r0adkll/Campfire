// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.openlibrary

import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.BookMatch
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

private const val EDITION_RESPONSE = """
{
  "works": [{"key": "/works/OL8479867W"}],
  "covers": [-1, 8739161],
  "publish_date": "Aug 31, 2010"
}
"""

private const val RATINGS_RESPONSE = """
{
  "summary": {"average": 4.35, "count": 132},
  "counts": {"1": 2, "2": 4, "3": 16, "4": 34, "5": 76}
}
"""

private const val EMPTY_RATINGS_RESPONSE = """
{
  "summary": {"average": null, "count": 0},
  "counts": {"1": 0, "2": 0, "3": 0, "4": 0, "5": 0}
}
"""

class OpenLibraryBookInfoProviderTest {

  private val requests = mutableListOf<String>()

  private fun provider(
    ratingsResponse: String = RATINGS_RESPONSE,
    editionStatus: HttpStatusCode = HttpStatusCode.OK,
  ): OpenLibraryBookInfoProvider {
    val client = HttpClient(
      MockEngine { request ->
        val url = request.url.toString()
        requests += url
        when {
          "/isbn/" in url -> respond(EDITION_RESPONSE, editionStatus)
          "/ratings.json" in url -> respond(ratingsResponse)
          else -> respond("", HttpStatusCode.NotFound)
        }
      },
    )
    return OpenLibraryBookInfoProvider(client)
  }

  @Test
  fun `only isbn bearing matches are servable`() {
    val provider = provider()

    assertThat(provider.canServe(BookMatch.Identifiers(isbn = "9780765393043", asin = null))).isTrue()
    assertThat(provider.canServe(BookMatch.Identifiers(isbn = null, asin = "B002RI9Z9E"))).isFalse()
    assertThat(provider.canServe(BookMatch.TitleAuthor("Title", "Author"))).isFalse()
  }

  @Test
  fun `ratings resolve through the edition's work`() = runTest {
    val result = provider().getBookInfo(BookMatch.Identifiers(isbn = "978-0-7653-9304-3", asin = null))

    val info = (result as BookInfoResult.Success).data
    assertThat(info.providerBookId).isEqualTo("/works/OL8479867W")
    assertThat(info.providerUrl).isEqualTo("https://openlibrary.org/works/OL8479867W")
    assertThat(info.rating).isEqualTo(4.35)
    assertThat(info.ratingsCount).isEqualTo(132)
    assertThat(info.ratingsDistribution).isEqualTo(mapOf(1 to 2, 2 to 4, 3 to 16, 4 to 34, 5 to 76))
    assertThat(info.reviewsCount).isNull()
    // Cover uses the first real cover id, skipping the -1 placeholder.
    assertThat(info.coverUrl).isEqualTo("https://covers.openlibrary.org/b/id/8739161-L.jpg")

    // The ISBN is normalized into the edition path.
    assertThat(requests.first().contains("/isbn/9780765393043.json")).isTrue()
  }

  @Test
  fun `a work without ratings is a miss`() = runTest {
    val result = provider(ratingsResponse = EMPTY_RATINGS_RESPONSE)
      .getBookInfo(BookMatch.Identifiers(isbn = "9780765393043", asin = null))

    assertThat(result).isEqualTo(BookInfoResult.NotFound)
  }

  @Test
  fun `an unknown isbn is a miss`() = runTest {
    val result = provider(editionStatus = HttpStatusCode.NotFound)
      .getBookInfo(BookMatch.Identifiers(isbn = "9999999999999", asin = null))

    assertThat(result).isEqualTo(BookInfoResult.NotFound)
    assertThat(requests.size).isEqualTo(1)
  }

  @Test
  fun `server errors surface as failures`() = runTest {
    val result = provider(editionStatus = HttpStatusCode.InternalServerError)
      .getBookInfo(BookMatch.Identifiers(isbn = "9780765393043", asin = null))

    assertThat(result).isInstanceOf(BookInfoResult.Failure::class)
  }

  @Test
  fun `an asin only match short circuits without a request`() = runTest {
    val result = provider().getBookInfo(BookMatch.Identifiers(isbn = null, asin = "B002RI9Z9E"))

    assertThat(result).isEqualTo(BookInfoResult.NotFound)
    assertThat(requests.size).isEqualTo(0)
  }
}
