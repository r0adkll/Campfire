// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.collections.ui.detail

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.collections.ui.list.FakeCollectionsRepository
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.common.test.user
import app.campfire.core.coroutines.LoadState
import app.campfire.core.session.UserSession
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.ItemDownloads
import app.campfire.offline.test.FakeDownloads
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class CollectionDetailPresenterTest {

  private val screen = CollectionDetailScreen("col_1", "Favorites")
  private val navigator = FakeNavigator(screen)
  private val repository = FakeCollectionsRepository()
  private val downloads = FakeDownloads()

  private val presenter = CollectionDetailPresenter(
    screen = screen,
    navigator = navigator,
    currentSession = UserSession.LoggedIn(user(id = "user_1")),
    collectionsRepository = repository,
    downloads = downloads,
    analytics = FakeAnalytics(),
  )

  private val book = libraryItem(id = "book")
  private val ebook = libraryItem(id = "ebook")

  @Test
  fun present_showsTheItems_withEachOnesDownloads() = runTest {
    val ebookDownload = Download(DownloadContent.Ebook(ebook.id), Download.State.Downloaded)
    repository.collectionItemsFlow.emit(listOf(book, ebook))
    downloads.downloads.value = listOf(ebookDownload)

    presenter.test {
      val state = awaitItemMatching {
        it.collectionContentState is LoadState.Loaded<*> && it.offlineStates.isNotEmpty()
      }

      assertThat(state.canEdit).isTrue()
      assertThat((state.collectionContentState as LoadState.Loaded).data).isEqualTo(listOf(book, ebook))
      assertThat(state.offlineStates).isEqualTo(mapOf(ebook.id to ItemDownloads(ebook.id, ebook = ebookDownload)))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun libraryItemClick_opensTheItem() = runTest {
    presenter.test {
      awaitItem().eventSink(CollectionDetailUiEvent.LibraryItemClick(book))

      assertThat(navigator.awaitNextScreen()).isEqualTo(LibraryItemScreen(book.id, book.id + "Favorites"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun deleteItems_removesThemFromTheCollection() = runTest {
    presenter.test {
      awaitItem().eventSink(CollectionDetailUiEvent.DeleteItems(listOf(book, ebook)))
      testScheduler.advanceUntilIdle()

      assertThat(repository.removedFromCollections).containsExactly(listOf(book.id, ebook.id) to "col_1")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun delete_deletesTheCollection_andLeaves() = runTest {
    presenter.test {
      awaitItem().eventSink(CollectionDetailUiEvent.Delete)

      navigator.awaitPop()
      assertThat(repository.deletedCollections).containsExactly("col_1")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun back_leaves() = runTest {
    presenter.test {
      awaitItem().eventSink(CollectionDetailUiEvent.Back)

      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }
  }
}

private suspend inline fun ReceiveTurbine<CollectionDetailUiState>.awaitItemMatching(
  predicate: (CollectionDetailUiState) -> Boolean,
): CollectionDetailUiState {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}
