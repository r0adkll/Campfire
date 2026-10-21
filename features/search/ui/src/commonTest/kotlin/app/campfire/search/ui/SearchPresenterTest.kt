// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.search.ui

import androidx.compose.foundation.text.input.TextFieldState
import app.campfire.analytics.test.FakeAnalytics
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.model.BasicSearchResult
import app.campfire.home.ui.author
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.series
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.ItemDownloads
import app.campfire.offline.test.FakeDownloads
import app.campfire.search.api.SearchRepository
import app.campfire.search.api.SearchResult
import app.campfire.search.api.ui.SearchResultNavEvent
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class SearchPresenterTest {

  private val book = libraryItem(id = "book")
  private val repository = FakeSearchRepository(SearchResult.Empty.copy(books = listOf(book)))
  private val downloads = FakeDownloads()
  private val navEvents = mutableListOf<SearchResultNavEvent>()

  private fun presenter(query: String) = SearchPresenter(
    searchRepository = repository,
    textFieldState = TextFieldState(query),
    onSearchEvent = { navEvents += it },
    downloads = downloads,
    analytics = FakeAnalytics(),
  )

  @Test
  fun present_searchesTheQuery_withEachResultsDownloads() = runTest {
    val download = Download(DownloadContent.Audiobook(book.id), Download.State.Downloaded)
    downloads.downloads.value = listOf(download)

    presenter("dune").test {
      val state = awaitItemMatching {
        (it.searchResult as? SearchResult.Success)?.books?.isNotEmpty() == true && it.offlineStates.isNotEmpty()
      }

      assertThat(state.query).isEqualTo("dune")
      assertThat(state.offlineStates).isEqualTo(mapOf(book.id to ItemDownloads(book.id, audiobook = download)))
      assertThat(repository.queries).containsExactly("dune")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_blankQuery_searchesNothing() = runTest {
    presenter(" ").test {
      assertThat(awaitItem().searchResult).isEqualTo(SearchResult.Empty)
      assertThat(repository.queries).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun resultClicks_navigate() = runTest {
    val author = author(id = "au_1", name = "Frank Herbert")
    val series = series(id = "se_1", name = "Dune")
    val narrator = BasicSearchResult("Scott Brick", 3)
    val tag = BasicSearchResult("Classics", 2)
    val genre = BasicSearchResult("Science Fiction", 9)

    presenter("").test {
      val eventSink = awaitItem().eventSink
      eventSink(SearchUiEvent.OnBookClick(book))
      eventSink(SearchUiEvent.OnAuthorClick(author))
      eventSink(SearchUiEvent.OnSeriesClick(series))
      eventSink(SearchUiEvent.OnNarratorClick(narrator))
      eventSink(SearchUiEvent.OnTagClick(tag))
      eventSink(SearchUiEvent.OnGenreClick(genre))

      // ContentFilters compare by identity, so the library filters are compared by kind and value
      assertThat(navEvents.map { it.describe() }).containsExactly(
        SearchResultNavEvent(LibraryItemScreen(book.id), false).toString(),
        SearchResultNavEvent(AuthorDetailScreen("au_1", "Frank Herbert"), false).toString(),
        SearchResultNavEvent(SeriesDetailScreen("se_1", "Dune"), false).toString(),
        "Narrators Scott Brick, resetRoot=true",
        "Tags Classics, resetRoot=true",
        "Genres Science Fiction, resetRoot=true",
      )
      cancelAndIgnoreRemainingEvents()
    }
  }
}

private fun SearchResultNavEvent.describe(): String {
  val filter = (screen as? LibraryScreen)?.filter ?: return toString()
  return "${filter::class.simpleName} ${filter.value}, resetRoot=$resetRoot"
}

private class FakeSearchRepository(private val result: SearchResult) : SearchRepository {

  val queries = mutableListOf<String>()

  override fun searchCurrentLibrary(query: String): Flow<SearchResult> {
    queries += query
    return flowOf(result)
  }
}

private suspend inline fun ReceiveTurbine<SearchUiState>.awaitItemMatching(
  predicate: (SearchUiState) -> Boolean,
): SearchUiState {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}
