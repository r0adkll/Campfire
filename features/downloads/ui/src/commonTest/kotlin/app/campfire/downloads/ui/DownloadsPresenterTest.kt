// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.common.screens.DownloadsScreen
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
import assertk.assertions.isEmpty
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
      // Rows show once their items are looked up
      var state = awaitItem()
      while (state.downloads.isEmpty()) state = awaitItem()

      assertThat(state.downloads).containsExactly(DownloadEntry.Book(book, bookDownload))
    }
  }

  @Test
  fun `shows nothing without downloads`() = runTest {
    presenter.test {
      assertThat(awaitItem().downloads).isEmpty()
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
    }
  }

  @Test
  fun `deleting a download removes it`() = runTest {
    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.DeleteDownload(DownloadEntry.Book(book, bookDownload)))

      assertThat(downloads.invocations).containsExactly(Invocation.Delete(bookDownload.content))
    }
  }

  @Test
  fun `back leaves the screen`() = runTest {
    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.Back)

      navigator.awaitPop()
    }
  }
}
