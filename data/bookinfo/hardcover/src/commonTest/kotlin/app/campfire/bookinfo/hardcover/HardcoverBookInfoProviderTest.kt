// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover

import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.BookMatch
import app.campfire.bookinfo.hardcover.auth.HardcoverTokenStorage
import app.campfire.bookinfo.hardcover.graphql.HardcoverGraphQl
import app.campfire.common.test.coroutines.TestDispatcherProvider
import app.campfire.common.test.user
import app.campfire.core.session.UserSession
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

private const val BOOK_RESPONSE = """
{"data": {"books": [{
  "id": 386446,
  "slug": "the-way-of-kings",
  "rating": 4.63,
  "ratings_count": 4109,
  "reviews_count": 422,
  "release_date": "2010-08-31",
  "cached_image": {"url": "https://assets.hardcover.app/cover.jpg"}
}]}}
"""

private const val REVIEWS_RESPONSE = """
{"data": {"user_books": [
  {
    "rating": 5.0,
    "review": "Loved it",
    "review_has_spoilers": false,
    "user": {
      "username": "reader",
      "name": "A Reader",
      "flair": "Supporter",
      "cached_image": {"url": "https://assets.hardcover.app/avatar.jpg"}
    }
  },
  {
    "rating": 4.0,
    "review": "Solid",
    "review_has_spoilers": true,
    "user": {"username": "plain", "name": "", "flair": null, "cached_image": {}}
  },
  {
    "rating": 3.0,
    "review": "   ",
    "review_has_spoilers": false,
    "user": {"username": "empty"}
  }
]}}
"""

class HardcoverBookInfoProviderTest {

  private val session: UserSession = UserSession.LoggedIn(user(id = "user-1"))
  private val requests = mutableListOf<String>()

  private suspend fun TestScope.provider(): HardcoverBookInfoProvider {
    val storage = HardcoverTokenStorage(
      hardcoverSettings = MapSettings(),
      dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
    )
    storage.link("user-1", "token", "me")
    val client = HttpClient(
      MockEngine { request ->
        val body = request.bodyText()
        requests += body
        when {
          "BookReviews" in body -> respond(REVIEWS_RESPONSE)
          else -> respond(BOOK_RESPONSE)
        }
      },
    )
    return HardcoverBookInfoProvider(
      graphQl = HardcoverGraphQl(client, storage, session),
      tokenStorage = storage,
      userSession = session,
    )
  }

  @Test
  fun `book info maps rating fields and cover`() = runTest {
    val result = provider().getBookInfo(BookMatch.Identifiers(isbn = "978-0-7653-9304-3", asin = null))

    val info = (result as BookInfoResult.Success).data
    assertThat(info.providerBookId).isEqualTo("386446")
    assertThat(info.providerUrl).isEqualTo("https://hardcover.app/books/the-way-of-kings")
    assertThat(info.rating).isEqualTo(4.63)
    assertThat(info.coverUrl).isEqualTo("https://assets.hardcover.app/cover.jpg")
    // ISBN is normalized before being sent as a query variable
    assertThat(requests.single().contains("9780765393043")).isEqualTo(true)
  }

  @Test
  fun `all available identifiers are sent in one lookup`() = runTest {
    val result = provider().getBookInfo(
      BookMatch.Identifiers(isbn = "9780765393043", asin = "B002RI9Z9E"),
    )

    assertThat(result is BookInfoResult.Success).isEqualTo(true)
    val body = requests.single()
    assertThat(body.contains("isbn_13")).isEqualTo(true)
    assertThat(body.contains("{asin: {_eq: \$asin}}")).isEqualTo(true)
    assertThat(body.contains("B002RI9Z9E")).isEqualTo(true)
  }

  @Test
  fun `asin only lookups omit the isbn predicate`() = runTest {
    provider().getBookInfo(BookMatch.Identifiers(isbn = null, asin = "B002RI9Z9E"))

    val body = requests.single()
    assertThat(body.contains("isbn_13")).isEqualTo(false)
    assertThat(body.contains("{asin: {_eq: \$asin}}")).isEqualTo(true)
  }

  @Test
  fun `reviews map reviewer name, avatar, and badge`() = runTest {
    val provider = provider()

    val result = provider.getReviews(BookMatch.Identifiers(isbn = "9780765393043", asin = null))

    val reviews = (result as BookInfoResult.Success).data
    assertThat(reviews.size).isEqualTo(2)

    val rich = reviews[0]
    assertThat(rich.author).isEqualTo("A Reader")
    assertThat(rich.avatarUrl).isEqualTo("https://assets.hardcover.app/avatar.jpg")
    assertThat(rich.badge).isEqualTo("Supporter")
    assertThat(rich.hasSpoilers).isEqualTo(false)

    // Blank display name falls back to username; empty image blob and null
    // flair map to nulls.
    val plain = reviews[1]
    assertThat(plain.author).isEqualTo("plain")
    assertThat(plain.avatarUrl).isNull()
    assertThat(plain.badge).isNull()
    assertThat(plain.hasSpoilers).isEqualTo(true)
  }

  @Test
  fun `resolving reviews after book info reuses the matched id`() = runTest {
    val provider = provider()
    val match = BookMatch.Identifiers(isbn = "9780765393043", asin = null)

    provider.getBookInfo(match)
    provider.getReviews(match)

    // One book lookup + one reviews call — no second book resolution.
    assertThat(requests.size).isEqualTo(2)
    assertThat(requests.count { "BookReviews" in it }).isEqualTo(1)
  }
}

private fun HttpRequestData.bodyText(): String = (body as TextContent).text
