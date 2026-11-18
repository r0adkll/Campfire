// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.PodcastEpisode
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import app.campfire.offline.test.FakeDownloads.Invocation
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class DownloadsPresenterTest {

  private val navigator = FakeNavigator(DownloadsScreen)
  private val downloads = FakeDownloads()
  private val libraryItemRepository = FakeLibraryItemRepository()

  private val book = libraryItem(id = "book")
  private val bookDownload = Download(
    content = DownloadContent.Audiobook(book.id),
    state = Download.State.Downloaded,
  )

  private val presenter = DownloadsPresenter(
    navigator = navigator,
    downloads = downloads,
    libraryItemRepository = libraryItemRepository,
    analytics = FakeAnalytics(),
  )

  @Test
  fun `lists what's downloaded`() = runTest {
    libraryItemRepository.libraryItem = book
    downloads.downloads.value = listOf(bookDownload)

    presenter.test {
      // Loading until the first row's item is looked up, never an empty list
      assertThat(awaitItem().downloads).isEqualTo(LoadState.Loading)
      assertThat(awaitItem().downloads).isEqualTo(LoadState.Loaded(listOf(DownloadEntry.Book(book, bookDownload))))
    }
  }

  @Test
  fun `shows nothing without downloads`() = runTest {
    presenter.test {
      assertThat(awaitItem().downloads).isEqualTo(LoadState.Loading)
      assertThat(awaitItem().downloads).isEqualTo(LoadState.Loaded(emptyList<DownloadEntry>()))
    }
  }

  @Test
  fun `a download whose item can't be found still shows`() = runTest {
    // The repository has no item to give
    downloads.downloads.value = listOf(bookDownload)

    presenter.test {
      assertThat(awaitItem().downloads).isEqualTo(LoadState.Loading)
      assertThat(awaitItem().downloads)
        .isEqualTo(LoadState.Loaded(listOf(DownloadEntry.Unavailable(bookDownload, sizeInBytes = 0L))))
    }
  }

  @Test
  fun `clicking a download whose item can't be found opens nothing`() = runTest {
    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.DownloadClick(DownloadEntry.Unavailable(bookDownload, 0L)))

      navigator.expectNoGoToEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `retrying a failed download starts it again`() = runTest {
    val failed = bookDownload.copy(state = Download.State.Failed)

    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.RetryDownload(DownloadEntry.Book(book, failed)))

      assertThat(downloads.invocations).containsExactly(Invocation.Download(listOf(failed.content)))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `deleting a download whose item can't be found removes everything of the item`() = runTest {
    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.DeleteDownload(DownloadEntry.Unavailable(bookDownload, 0L)))
      testScheduler.runCurrent()

      assertThat(downloads.invocations).containsExactly(Invocation.DeleteAll(book.id))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `clicking an episode opens it`() = runTest {
    val episode = PodcastEpisode(
      id = "ep_1",
      libraryItemId = "podcast",
      podcastId = "podcast_media",
      title = "Episode One",
      addedAtMillis = 0L,
      updatedAtMillis = 0L,
      durationInMillis = 0L,
      sizeInBytes = 0L,
    )
    val entry = DownloadEntry.Episode(
      libraryItem = libraryItem(id = "podcast"),
      episode = episode,
      download = Download(
        content = DownloadContent.Episode("podcast", episode.id),
        state = Download.State.Downloaded,
      ),
    )

    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.DownloadClick(entry))

      assertThat(navigator.awaitNextScreen())
        .isEqualTo(LibraryItemScreen(libraryItemId = "podcast", episodeId = "ep_1"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `deleting a download removes it`() = runTest {
    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.DeleteDownload(DownloadEntry.Book(book, bookDownload)))

      assertThat(downloads.invocations).containsExactly(Invocation.Delete(bookDownload.content))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `back leaves the screen`() = runTest {
    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.Back)

      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }
  }
}
