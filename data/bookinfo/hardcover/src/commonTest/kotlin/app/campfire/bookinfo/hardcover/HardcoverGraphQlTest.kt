// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover

import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.hardcover.auth.HardcoverTokenStorage
import app.campfire.bookinfo.hardcover.graphql.BooksData
import app.campfire.bookinfo.hardcover.graphql.HardcoverGraphQl
import app.campfire.bookinfo.hardcover.graphql.HardcoverResult
import app.campfire.common.test.coroutines.TestDispatcherProvider
import app.campfire.common.test.user
import app.campfire.core.session.UserSession
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class HardcoverGraphQlTest {

  private val settings = MapSettings()
  private val session: UserSession = UserSession.LoggedIn(user(id = "user-1"))

  private fun graphQl(
    scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
    handler: MockRequestHandleScope.() -> io.ktor.client.request.HttpResponseData,
  ): Pair<HardcoverGraphQl, HardcoverTokenStorage> {
    val storage = HardcoverTokenStorage(
      hardcoverSettings = settings,
      dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(scheduler)),
    )
    val client = HttpClient(MockEngine { handler() })
    return HardcoverGraphQl(client, storage, session) to storage
  }

  @Test
  fun `missing token short circuits to not linked without a request`() = runTest {
    var requested = false
    val (graphQl, _) = graphQl(testScheduler) {
      requested = true
      respond("{}")
    }

    val result = graphQl.execute("query {}", deserializer = BooksData.serializer())

    assertThat(result).isEqualTo(HardcoverResult.NotLinked)
    assertThat(requested).isEqualTo(false)
  }

  @Test
  fun `successful envelope decodes the data element`() = runTest {
    val (graphQl, storage) = graphQl(testScheduler) {
      respond(
        """{"data": {"books": [{"id": 386446, "slug": "the-way-of-kings", "rating": 4.63, "ratings_count": 4109}]}}""",
      )
    }
    storage.link("user-1", "token", "reader")

    val result = graphQl.execute("query {}", deserializer = BooksData.serializer())

    val success = result as HardcoverResult.Success
    assertThat(success.data.books.single().id).isEqualTo(386446L)
    assertThat(success.data.books.single().rating).isNotNull()
  }

  @Test
  fun `graphql errors with http 200 are surfaced as errors`() = runTest {
    val (graphQl, storage) = graphQl(testScheduler) {
      respond("""{"errors": [{"message": "field 'nope' not found"}], "data": null}""")
    }
    storage.link("user-1", "token", null)

    val result = graphQl.execute("query {}", deserializer = BooksData.serializer())

    val errors = result as HardcoverResult.GraphQlErrors
    assertThat(errors.messages).isEqualTo(listOf("field 'nope' not found"))
  }

  @Test
  fun `unauthorized marks the stored token invalid`() = runTest {
    val (graphQl, storage) = graphQl(testScheduler) {
      respond("", HttpStatusCode.Unauthorized)
    }
    storage.link("user-1", "token", null)

    val result = graphQl.execute("query {}", deserializer = BooksData.serializer())

    assertThat(result).isEqualTo(HardcoverResult.TokenInvalid)
    assertThat(storage.observeLinkState("user-1").first()).isEqualTo(ProviderLinkState.Invalid)
  }

  @Test
  fun `too many requests carries the retry delay`() = runTest {
    val (graphQl, storage) = graphQl(testScheduler) {
      respond("", HttpStatusCode.TooManyRequests, headersOf("Retry-After", "42"))
    }
    storage.link("user-1", "token", null)

    val result = graphQl.execute("query {}", deserializer = BooksData.serializer())

    assertThat(result).isEqualTo(HardcoverResult.RateLimited(42.seconds))
  }

  @Test
  fun `network failures are wrapped`() = runTest {
    val (graphQl, storage) = graphQl(testScheduler) {
      respond("oops", HttpStatusCode.InternalServerError)
    }
    storage.link("user-1", "token", null)

    val result = graphQl.execute("query {}", deserializer = BooksData.serializer())

    assertThat(result).isInstanceOf(HardcoverResult.NetworkFailure::class)
  }

  @Test
  fun `relinking clears the invalid state`() = runTest {
    val (_, storage) = graphQl(testScheduler) { respond("") }
    storage.link("user-1", "token", null)
    storage.markInvalid("user-1")
    assertThat(storage.observeLinkState("user-1").first()).isEqualTo(ProviderLinkState.Invalid)

    storage.link("user-1", "fresh-token", "reader")

    assertThat(storage.observeLinkState("user-1").first())
      .isEqualTo(ProviderLinkState.Linked("reader"))
    assertThat(storage.getToken("user-1")).isEqualTo("fresh-token")

    storage.unlink("user-1")
    assertThat(storage.observeLinkState("user-1").first()).isEqualTo(ProviderLinkState.NotLinked)
    assertThat(storage.getToken("user-1") == null).isTrue()
  }
}
