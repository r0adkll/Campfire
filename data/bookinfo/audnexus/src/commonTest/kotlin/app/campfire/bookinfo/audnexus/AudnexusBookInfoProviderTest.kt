// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.audnexus

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

private const val BOOK_RESPONSE = """
{
  "asin": "B002V0QK4C",
  "title": "The Way of Kings",
  "rating": "4.79",
  "releaseDate": "2010-08-31T00:00:00.000Z",
  "image": "https://m.media-amazon.com/images/I/91KzZWpgmyL.jpg",
  "runtimeLengthMin": 2734,
  "genres": [{"asin": "18580606011", "name": "Science Fiction & Fantasy", "type": "genre"}]
}
"""

private const val UNRATED_RESPONSE = """
{"asin": "B002V0QK4C", "title": "Obscure Book", "rating": "0"}
"""

class AudnexusBookInfoProviderTest {

  private val requests = mutableListOf<String>()

  private fun provider(
    response: String = BOOK_RESPONSE,
    status: HttpStatusCode = HttpStatusCode.OK,
  ): AudnexusBookInfoProvider {
    val client = HttpClient(
      MockEngine { request ->
        requests += request.url.toString()
        respond(response, status)
      },
    )
    return AudnexusBookInfoProvider(client)
  }

  @Test
  fun `only asin bearing matches are servable`() {
    val provider = provider()

    assertThat(provider.canServe(BookMatch.Identifiers(isbn = null, asin = "B002V0QK4C"))).isTrue()
    assertThat(provider.canServe(BookMatch.Identifiers(isbn = "9780765393043", asin = null))).isFalse()
    assertThat(provider.canServe(BookMatch.TitleAuthor("Title", "Author"))).isFalse()
  }

  @Test
  fun `the audible rating and metadata are mapped`() = runTest {
    val result = provider().getBookInfo(BookMatch.Identifiers(isbn = null, asin = " b002v0qk4c "))

    val info = (result as BookInfoResult.Success).data
    assertThat(info.providerBookId).isEqualTo("B002V0QK4C")
    assertThat(info.providerUrl).isEqualTo("https://www.audible.com/pd/B002V0QK4C")
    assertThat(info.rating).isEqualTo(4.79)
    assertThat(info.ratingsCount).isNull()
    assertThat(info.ratingsDistribution).isNull()
    assertThat(info.coverUrl).isEqualTo("https://m.media-amazon.com/images/I/91KzZWpgmyL.jpg")
    // The ASIN is normalized (trimmed, uppercased) into the request path.
    assertThat(requests.single().endsWith("/books/B002V0QK4C")).isTrue()
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
