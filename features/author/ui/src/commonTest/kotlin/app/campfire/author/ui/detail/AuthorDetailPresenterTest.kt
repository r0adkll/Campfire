// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.author.ui.detail

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.author.api.AuthorPager
import app.campfire.author.api.AuthorRepository
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Author
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.home.ui.author
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.ItemDownloads
import app.campfire.offline.test.FakeDownloads
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class AuthorDetailPresenterTest {

  private val screen = AuthorDetailScreen("au_1", "Brandon Sanderson")
  private val navigator = FakeNavigator(screen)
  private val book = libraryItem(id = "book")
  private val author = author(id = "au_1", name = "Brandon Sanderson", libraryItems = listOf(book))
  private val downloads = FakeDownloads()

  private val presenter = AuthorDetailPresenter(
    screen = screen,
    navigator = navigator,
    authorRepository = FakeAuthorRepository(author),
    downloads = downloads,
    analytics = FakeAnalytics(),
  )

  @Test
  fun present_showsTheAuthor_withTheirBooksDownloads() = runTest {
    val download = Download(DownloadContent.Audiobook(book.id), Download.State.Downloading, progress = 0.5f)
    downloads.downloads.value = listOf(download)

    presenter.test {
      val state = awaitItemMatching {
        it.authorContentState is LoadState.Loaded<*> && it.offlineStates.isNotEmpty()
      }

      assertThat((state.authorContentState as LoadState.Loaded).data).isEqualTo(author)
      assertThat(state.offlineStates).isEqualTo(mapOf(book.id to ItemDownloads(book.id, audiobook = download)))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun libraryItemClick_opensTheItem() = runTest {
    presenter.test {
      awaitItem().eventSink(AuthorDetailUiEvent.LibraryItemClick(book))

      assertThat(navigator.awaitNextScreen()).isEqualTo(LibraryItemScreen(book.id))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun back_leaves() = runTest {
    presenter.test {
      awaitItem().eventSink(AuthorDetailUiEvent.Back)

      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }
  }
}

private class FakeAuthorRepository(private val author: Author) : AuthorRepository {
  override fun observeAuthors(): Flow<List<Author>> = flowOf(listOf(author))
  override fun createAuthorsPager(user: User, sortMode: ContentSortMode, sortDirection: SortDirection): AuthorPager =
    TODO()
  override fun observeFilteredAuthorsCount(sortMode: ContentSortMode, sortDirection: SortDirection): Flow<Int?> =
    flowOf(1)
  override fun observeAuthor(authorId: String): Flow<Author> = flowOf(author)
}

private suspend inline fun ReceiveTurbine<AuthorDetailUiState>.awaitItemMatching(
  predicate: (AuthorDetailUiState) -> Boolean,
): AuthorDetailUiState {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}
