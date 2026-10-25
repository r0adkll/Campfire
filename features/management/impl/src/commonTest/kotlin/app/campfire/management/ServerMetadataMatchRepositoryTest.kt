// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.SeriesSequence
import app.campfire.home.ui.authorMetadata
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.SeriesEntry
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.management.api.match.BookMatch
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.api.match.QuickMatchResult
import app.campfire.network.ApiException
import app.campfire.network.envelopes.QuickMatchRequest
import app.campfire.network.envelopes.QuickMatchResponse
import app.campfire.network.models.BookMatchResult
import app.campfire.network.models.BookMatchSeries
import app.campfire.network.models.SearchProvider
import app.campfire.network.models.SearchProviders
import app.campfire.network.models.SearchProvidersResponse
import app.campfire.network.test.FakeAudioBookShelfApi
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.first
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class ServerMetadataMatchRepositoryTest {

  private val api = FakeAudioBookShelfApi()
  private val items = FakeLibraryItemRepository()
  private val repository = ServerMetadataMatchRepository(api, items)

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

  @Test
  fun `a quick match says what it may replace, and what it changed`() = runTest {
    val before = book(mediaMetadata(subtitle = null, genres = listOf("Fiction")))
    matchesInto(book(mediaMetadata(subtitle = "A Novel", genres = listOf("Science Fiction"))), from = before)

    val result = repository.quickMatchBook("item-1", "audible", replaceDetails = true, replaceCover = false)

    assertThat(api.quickMatches).containsExactly(
      "item-1" to QuickMatchRequest("audible", overrideDetails = true, overrideCover = false),
    )
    assertThat(result.getOrThrow()).isEqualTo(
      QuickMatchResult.Updated(
        undo = BookDetailsUpdate(subtitle = "", genres = listOf("Fiction")),
        coverChanged = false,
      ),
    )
  }

  @Test
  fun `names that only differ in case or order didn't change`() = runTest {
    val before = book(
      mediaMetadata(
        authors = listOf(authorMetadata(name = "Andy Weir"), authorMetadata(name = "R. C. Bray")),
        seriesSequence = SeriesSequence("s1", "The Martian", 1.0),
      ),
    )
    val after = book(
      mediaMetadata(
        authors = listOf(authorMetadata(name = "r. c. bray"), authorMetadata(name = "Andy Weir")),
        seriesSequence = SeriesSequence("s2", "the martian", 1.0),
        narrators = listOf("R. C. Bray"),
      ),
    )
    matchesInto(after, from = before)

    val result = repository.quickMatchBook("item-1", "audible", replaceDetails = true, replaceCover = false)

    assertThat(result.getOrThrow()).isEqualTo(
      QuickMatchResult.Updated(undo = BookDetailsUpdate(narrators = listOf("Voice Actor Prime")), coverChanged = false),
    )
  }

  @Test
  fun `a cover given to a book without one changed`() = runTest {
    matchesInto(book(coverPath = "/metadata/items/item-1/cover.jpg"), from = book(coverPath = null))

    val result = repository.quickMatchBook("item-1", "audible", replaceDetails = false, replaceCover = false)

    assertThat(result.getOrThrow()).isEqualTo(QuickMatchResult.Updated(BookDetailsUpdate(), coverChanged = true))
  }

  @Test
  fun `a change that left the details alone was the cover`() = runTest {
    matchesInto(book(), from = book())

    val result = repository.quickMatchBook("item-1", "audible", replaceDetails = false, replaceCover = true)

    assertThat(result.getOrThrow()).isEqualTo(QuickMatchResult.Updated(BookDetailsUpdate(), coverChanged = true))
  }

  @Test
  fun `a changed book that can't be fetched again can't be undone`() = runTest {
    items.fetchResult = Result.success(book())
    api.quickMatchResult = { _, _ ->
      items.fetchResult = Result.failure(ApiException(500))
      Result.success(QuickMatchResponse(updated = true))
    }

    val result = repository.quickMatchBook("item-1", "audible", replaceDetails = false, replaceCover = false)

    assertThat(result.getOrThrow()).isEqualTo(QuickMatchResult.Updated(undo = null, coverChanged = false))
  }

  @Test
  fun `nothing found, and nothing to change, are told apart`() = runTest {
    items.fetchResult = Result.success(book())

    api.quickMatchResult = { _, _ -> Result.success(QuickMatchResponse(warning = "No audible match found")) }
    val notFound = repository.quickMatchBook("item-1", "audible", replaceDetails = false, replaceCover = false)
    api.quickMatchResult = { _, _ -> Result.success(QuickMatchResponse(updated = false)) }
    val unchanged = repository.quickMatchBook("item-1", "audible", replaceDetails = false, replaceCover = false)

    assertThat(notFound.getOrThrow()).isEqualTo(QuickMatchResult.NoMatch)
    assertThat(unchanged.getOrThrow()).isEqualTo(QuickMatchResult.NothingChanged)
  }

  @Test
  fun `a refused quick match is the account's permission`() = runTest {
    items.fetchResult = Result.success(book())
    api.quickMatchResult = { _, _ -> Result.failure(ApiException(403)) }

    val result = repository.quickMatchBook("item-1", "audible", replaceDetails = false, replaceCover = false)

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<ItemChangeNotAllowedException>()
  }

  @Test
  fun `a book that can't be fetched first isn't matched`() = runTest {
    items.fetchResult = Result.failure(ApiException(500))

    val result = repository.quickMatchBook("item-1", "audible", replaceDetails = false, replaceCover = false)

    assertThat(result.isFailure).isTrue()
    assertThat(api.quickMatches).isEmpty()
  }

  /** The book is [from] until the server matches it, and [into] after */
  private fun matchesInto(into: LibraryItem, from: LibraryItem) {
    items.fetchResult = Result.success(from)
    api.quickMatchResult = { _, _ ->
      items.fetchResult = Result.success(into)
      Result.success(QuickMatchResponse(updated = true))
    }
  }

  private fun book(
    metadata: Media.Metadata.Book = mediaMetadata(),
    coverPath: String? = "/metadata/items/item-1/cover.jpg",
  ) = libraryItem(id = "item-1", media = media(metadata = metadata, coverPath = coverPath))
}
