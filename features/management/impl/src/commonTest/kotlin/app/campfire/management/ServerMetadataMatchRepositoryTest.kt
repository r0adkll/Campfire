// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management

import app.campfire.libraries.api.SeriesEntry
import app.campfire.management.api.match.BookMatch
import app.campfire.management.api.match.MatchProvider
import app.campfire.network.ApiException
import app.campfire.network.models.BookMatchResult
import app.campfire.network.models.BookMatchSeries
import app.campfire.network.models.SearchProvider
import app.campfire.network.models.SearchProviders
import app.campfire.network.models.SearchProvidersResponse
import app.campfire.network.test.FakeAudioBookShelfApi
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.first
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class ServerMetadataMatchRepositoryTest {

  private val api = FakeAudioBookShelfApi()
  private val repository = ServerMetadataMatchRepository(api)

  @Test
  fun `the server's own sources are offered`() = runTest {
    api.searchProvidersResult = {
      Result.success(
        SearchProvidersResponse(
          SearchProviders(
            books = listOf(SearchProvider("audible", "Audible.com"), SearchProvider("custom-1", "My provider")),
          ),
        ),
      )
    }

    val providers = repository.getBookProviders().getOrThrow()

    assertThat(providers).containsExactly(
      MatchProvider("audible", "Audible.com"),
      MatchProvider("custom-1", "My provider"),
    )
  }

  @Test
  fun `a server too old to list its sources gets the built-in ones`() = runTest {
    api.searchProvidersResult = { Result.failure(ApiException(404)) }

    val providers = repository.getBookProviders().getOrThrow()

    assertThat(providers.map { it.id }).first().isEqualTo("audible")
    assertThat(providers.any { it.id == "google" }).isEqualTo(true)
  }

  @Test
  fun `other failures to list sources come back as they were`() = runTest {
    api.searchProvidersResult = { Result.failure(ApiException(500)) }

    val providers = repository.getBookProviders()

    assertThat(providers.exceptionOrNull()).isNotNull().isInstanceOf<ApiException>()
  }

  @Test
  fun `a search asks for the item's matches, trimmed, and remembers the source`() = runTest {
    api.searchBooksResult = { Result.success(emptyList()) }

    repository.searchBooks("item-1", "audible.uk", "  Children of Ruin ", " ")

    assertThat(api.bookSearches).containsExactly(
      FakeAudioBookShelfApi.BookSearch("audible.uk", "Children of Ruin", null, "item-1"),
    )
    assertThat(repository.lastProvider).isEqualTo("audible.uk")
  }

  @Test
  fun `results come back as matches, whatever shape each source uses`() = runTest {
    api.searchBooksResult = {
      Result.success(
        listOf(
          BookMatchResult(
            title = "Children of Ruin",
            subtitle = " ",
            author = JsonPrimitive("Adrian Tchaikovsky, Someone Else"),
            narrator = JsonArray(listOf(JsonPrimitive("Mel Hudson"))),
            publishedYear = "2019",
            cover = "https://m.media-amazon.com/images/cover.jpg",
            genres = JsonArray(listOf(JsonPrimitive("Science Fiction"), JsonPrimitive("Space Opera"))),
            tags = JsonPrimitive("Hard SF,  Evolution"),
            series = listOf(BookMatchSeries("Children of Time", "2"), BookMatchSeries(null, "3")),
            duration = 900.0,
            matchConfidence = 0.96,
            abridged = false,
          ),
          // Audible's way of saying it doesn't know the length
          BookMatchResult(title = "Unknown length", duration = 0.0),
        ),
      )
    }

    val matches = repository.searchBooks("item-1", "audible", "Children of Ruin", "Tchaikovsky").getOrThrow()

    assertThat(matches).containsExactly(
      BookMatch(
        title = "Children of Ruin",
        authors = listOf("Adrian Tchaikovsky", "Someone Else"),
        narrators = listOf("Mel Hudson"),
        publishedYear = "2019",
        coverUrl = "https://m.media-amazon.com/images/cover.jpg",
        genres = listOf("Science Fiction", "Space Opera"),
        tags = listOf("Hard SF", "Evolution"),
        series = listOf(SeriesEntry("Children of Time", "2")),
        duration = 900.minutes,
        confidence = 0.96f,
        abridged = false,
      ),
      BookMatch(title = "Unknown length"),
    )
  }
}
