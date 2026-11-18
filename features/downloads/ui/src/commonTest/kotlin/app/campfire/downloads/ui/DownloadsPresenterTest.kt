// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.PlaybackControllerSession
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.PodcastEpisode
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import app.campfire.offline.test.FakeDownloads.Invocation
import app.campfire.reader.api.screen.ReaderScreen
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.prop
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DownloadsPresenterTest {

  private val navigator = FakeNavigator(DownloadsScreen)
  private val downloads = FakeDownloads()
  private val libraryItemRepository = FakeLibraryItemRepository()
  private val playbackController = FakePlaybackController()
  private val userScope = TestScope()

  private val book = libraryItem(id = "book")
  private val bookDownload = Download(
    content = DownloadContent.Audiobook(book.id),
    state = Download.State.Downloaded,
  )

  private val presenter = DownloadsPresenter(
    navigator = navigator,
    downloads = downloads,
    libraryItemRepository = libraryItemRepository,
    playbackController = playbackController,
    analytics = FakeAnalytics(),
    userScope = CoroutineScopeHolder { userScope },
  )

  private val episode = PodcastEpisode(
    id = "ep_1",
    libraryItemId = "podcast",
    podcastId = "podcast_media",
    title = "Episode One",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = 0L,
  )
  private val episodeEntry = DownloadEntry.Episode(
    libraryItem = libraryItem(id = "podcast"),
    episode = episode,
    download = Download(
      content = DownloadContent.Episode("podcast", episode.id),
      state = Download.State.Downloaded,
    ),
  )

  private val other = libraryItem(id = "other")
  private val otherDownload = Download(
    content = DownloadContent.Audiobook(other.id),
    state = Download.State.Downloaded,
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
  fun `clicking an episode opens it`() = runTest {
    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.DownloadClick(episodeEntry))

      assertThat(navigator.awaitNextScreen())
        .isEqualTo(LibraryItemScreen(libraryItemId = "podcast", episodeId = "ep_1"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `playing a book or an episode starts it`() = runTest {
    presenter.test {
      val state = awaitItem()

      state.eventSink(DownloadsUiEvent.PlayClick(DownloadEntry.Book(book, bookDownload)))
      assertThat(playbackController.session).isInstanceOf<PlaybackControllerSession.Started>()
        .prop(PlaybackControllerSession.Started::itemId).isEqualTo(book.id)

      state.eventSink(DownloadsUiEvent.PlayClick(episodeEntry))
      assertThat(playbackController.session).isInstanceOf<PlaybackControllerSession.Started>()
        .prop(PlaybackControllerSession.Started::episodeId).isEqualTo("ep_1")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `reading an ebook opens the reader`() = runTest {
    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.PlayClick(DownloadEntry.Ebook(book, bookDownload)))

      assertThat(navigator.awaitNextScreen()).isEqualTo(ReaderScreen(book.id))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a deleted download leaves the list, and is only removed once it can't be undone`() = runTest {
    libraryItemRepository.libraryItem = book
    downloads.downloads.value = listOf(bookDownload)
    val entry = DownloadEntry.Book(book, bookDownload)

    presenter.test {
      val listed = awaitLoaded()
      listed.eventSink(DownloadsUiEvent.DeleteDownload(entry))

      val pending = awaitItem()
      assertThat(pending.downloads).isEqualTo(LoadState.Loaded(emptyList<DownloadEntry>()))
      assertThat(pending.pendingDelete).isEqualTo(entry)
      assertThat(downloads.invocations).isEmpty()

      pending.eventSink(DownloadsUiEvent.CommitDelete(entry))

      // Stays off the list while its download is being removed
      val committed = awaitItem()
      assertThat(committed.downloads).isEqualTo(LoadState.Loaded(emptyList<DownloadEntry>()))
      assertThat(committed.pendingDelete).isNull()
      assertThat(downloads.invocations).containsExactly(Invocation.Delete(bookDownload.content))
    }
  }

  @Test
  fun `undoing a delete puts the download back and removes nothing`() = runTest {
    libraryItemRepository.libraryItem = book
    downloads.downloads.value = listOf(bookDownload)
    val entry = DownloadEntry.Book(book, bookDownload)

    presenter.test {
      awaitLoaded().eventSink(DownloadsUiEvent.DeleteDownload(entry))
      awaitItem().eventSink(DownloadsUiEvent.UndoDelete(entry))

      val undone = awaitItem()
      assertThat(undone.downloads).isEqualTo(LoadState.Loaded(listOf(entry)))
      assertThat(undone.pendingDelete).isNull()
      assertThat(downloads.invocations).isEmpty()
    }
  }

  @Test
  fun `deleting another download removes the one before it`() = runTest {
    val first = DownloadEntry.Book(book, bookDownload)
    val second = DownloadEntry.Book(other, otherDownload)

    presenter.test {
      val state = awaitItem()
      state.eventSink(DownloadsUiEvent.DeleteDownload(first))
      state.eventSink(DownloadsUiEvent.DeleteDownload(second))

      assertThat(downloads.invocations).containsExactly(Invocation.Delete(bookDownload.content))
      var latest = awaitItem()
      while (latest.pendingDelete != second) latest = awaitItem()

      // Too late to undo the first
      latest.eventSink(DownloadsUiEvent.UndoDelete(first))
      expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `leaving the screen removes a delete still waiting to be undone`() = runTest {
    presenter.test {
      awaitItem().eventSink(DownloadsUiEvent.DeleteDownload(DownloadEntry.Book(book, bookDownload)))
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(downloads.invocations).containsExactly(Invocation.Delete(bookDownload.content))
  }

  @Test
  fun `deleting a download whose item can't be found removes everything of the item`() = runTest {
    val entry = DownloadEntry.Unavailable(bookDownload, 0L)

    presenter.test {
      val state = awaitItem()
      state.eventSink(DownloadsUiEvent.DeleteDownload(entry))
      state.eventSink(DownloadsUiEvent.CommitDelete(entry))
      userScope.testScheduler.runCurrent()

      assertThat(downloads.invocations).containsExactly(Invocation.DeleteAll(book.id))
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

  /** The first state with the list loaded. */
  private suspend fun ReceiveTurbine<DownloadsUiState>.awaitLoaded(): DownloadsUiState {
    var state = awaitItem()
    while (state.downloads !is LoadState.Loaded) state = awaitItem()
    return state
  }
}
