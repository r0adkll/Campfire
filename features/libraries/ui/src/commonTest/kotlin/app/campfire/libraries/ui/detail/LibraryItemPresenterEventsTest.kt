// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import app.campfire.analytics.events.AnalyticEvent
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.PlaybackControllerSession
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.test.assert.firstInstanceOf
import app.campfire.common.test.session
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.SeriesSequence
import app.campfire.home.ui.authorMetadata
import app.campfire.home.ui.chapter
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.RescanResult
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.management.api.screen.ItemEditorScreen
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import app.campfire.reader.api.screen.ReaderScreen
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.user.test.FakeMediaProgressRepository
import app.cash.burst.Burst
import app.cash.burst.burstValues
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.single
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class LibraryItemPresenterEventsTest : BaseLibraryItemPresenterTest() {

  @Burst
  @Test
  fun eventSinkTests(
    eventTest: EventTest = burstValues(
      TimeInBookChange,
      StopDownloadClick,
      RemoveDownloadClick,
      DownloadClick,
      ChapterClickWithDifferentSessionAndPlayer,
      ChapterClickWithSessionAndPlayer,
      ChapterClickWithSessionWithoutPlayer,
      ChapterClickWithoutSessionOrPlayer,
      MarkNotFinished,
      MarkFinished,
      DiscardProgress,
      SeriesClick,
      NarratorClick,
      AuthorClick,
      PlayClick,
      PlayClickWithMethod,
      DeleteItemClickSoft,
      DeleteItemClickHard,
      EditClick,
      OnBack,
    ),
  ) = runTest {
    val libraryItem = emptyLibraryItem()
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = libraryItem,
    )

    eventTest.setup(this@LibraryItemPresenterEventsTest)

    presenter.test {
      skipItems(1)
      val item = awaitItem()

      item.eventSink(eventTest.event)

      eventTest.assert(this@LibraryItemPresenterEventsTest)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun ebookOnlyItem_playOpensReader_andIgnoresQueueEvents() = runTest {
    val ebookItem = emptyLibraryItem(numTracks = 0, ebookFormat = "epub")
    libraryItemRepository.libraryItemFlow.emit(ebookItem)

    presenter.test {
      skipItems(1)
      val item = awaitItem()

      item.eventSink(LibraryItemUiEvent.PlayClick(null))
      item.eventSink(LibraryItemUiEvent.AddToQueue)

      assertThat(playbackController.session)
        .isEqualTo(PlaybackControllerSession.None)
      assertThat(sessionQueue.queue).isEmpty()
      assertThat(analytics.events).isEmpty()
      assertThat(navigator.awaitNextScreen()).isEqualTo(ReaderScreen(ebookItem.id))

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun ebookOnlyItem_downloadsAndDeletesItsEbook() = runTest {
    val ebookItem = emptyLibraryItem(numTracks = 0, ebookFormat = "epub")
    libraryItemRepository.libraryItemFlow.emit(ebookItem)

    presenter.test {
      skipItems(1)
      val item = awaitItem()

      item.eventSink(LibraryItemUiEvent.DownloadClick())
      item.eventSink(LibraryItemUiEvent.StopDownloadClick)
      item.eventSink(LibraryItemUiEvent.RemoveDownloadClick)

      // An ebook's single file can't be stopped part way, so stopping is ignored
      assertThat(downloads.invocations).containsExactly(
        FakeDownloads.Invocation.Download(listOf(DownloadContent.Ebook(ebookItem.id))),
        FakeDownloads.Invocation.Delete(DownloadContent.Ebook(ebookItem.id)),
      )

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun readClick_opensTheReaderForAnItemWithAnEbook() = runTest {
    val audiobook = emptyLibraryItem(ebookFormat = "epub")
    libraryItemRepository.libraryItemFlow.emit(audiobook)

    presenter.test {
      skipItems(1)
      awaitItem().eventSink(LibraryItemUiEvent.ReadClick)

      assertThat(navigator.awaitNextScreen()).isEqualTo(ReaderScreen(audiobook.id))

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun downloadClick_withoutDownloadPermission_isIgnored() = runTest {
    val user = userRepository.currentStatefulUserFlow.value
    userRepository.currentStatefulUserFlow.value = user.copy(
      permissions = user.permissions.copy(download = false),
    )
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())

    presenter.test {
      skipItems(1)
      val item = awaitItem()

      item.eventSink(LibraryItemUiEvent.DownloadClick())

      assertThat(downloads.invocations).isEmpty()
      assertThat(analytics.events).isEmpty()

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun deleteItemClick_onFailure_saysSoInTheSnackbar() = runTest {
    val libraryItem = emptyLibraryItem()
    libraryItemRepository.libraryItemFlow.emit(libraryItem)
    libraryItemRepository.deleteResult = Result.failure(IllegalStateException("boom"))

    presenter.test {
      skipItems(1)
      val item = awaitItem()

      item.eventSink(LibraryItemUiEvent.DeleteItemClick(hardDelete = false))

      awaitSnackbar("Couldn't delete this. Check your connection and try again.")
      assertThat(libraryItemRepository.deleteInvocations).hasSize(1)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun deleteItemClick_whenNotAllowed_saysToAskAnAdmin() = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    libraryItemRepository.deleteResult = Result.failure(ItemChangeNotAllowedException())

    presenter.test {
      skipItems(1)
      awaitItem().eventSink(LibraryItemUiEvent.DeleteItemClick(hardDelete = true))

      awaitSnackbar("Your account isn't allowed to delete this. Ask your server's admin.")
      // Still here, as nothing was deleted
      navigator.expectNoEvents()

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Burst
  @Test
  fun rescanClick_saysWhatTheServerFound(
    outcome: RescanOutcome = burstValues(
      RescanOutcome(Result.success(RescanResult.Updated), "Rescanned, and updated from its files"),
      RescanOutcome(Result.success(RescanResult.UpToDate), "Rescanned. It already matched its files."),
      RescanOutcome(Result.success(RescanResult.Nothing), "Your server couldn't find this to rescan it"),
      RescanOutcome(
        Result.failure(ItemChangeNotAllowedException()),
        "Your account isn't allowed to rescan this. Ask your server's admin.",
      ),
      RescanOutcome(
        Result.failure(IllegalStateException("offline")),
        "Couldn't rescan this. Check your connection and try again.",
      ),
    ),
  ) = runTest {
    libraryItemRepository.libraryItemFlow.emit(emptyLibraryItem())
    libraryItemRepository.rescanResult = outcome.result

    presenter.test {
      skipItems(1)
      awaitItem().eventSink(LibraryItemUiEvent.RescanClick)

      awaitSnackbar(outcome.message)
      assertThat(libraryItemRepository.rescanInvocations).containsExactly(TestLibraryItemId)

      cancelAndIgnoreRemainingEvents()
    }
  }

  /**
   * Waits for the app's snackbar to say [message]. Its words load off the test's own thread, so
   * this polls from another, reading only the snackbar host's state there.
   */
  private suspend fun awaitSnackbar(message: String) = withContext(Dispatchers.Default) {
    withTimeout(5.seconds) {
      while (snackbarHost.currentSnackbarData?.visuals?.message != message) delay(10)
    }
  }
}

data class RescanOutcome(
  val result: Result<RescanResult>,
  val message: String,
)

data class EventTest(
  val event: LibraryItemUiEvent,
  val setup: suspend BaseLibraryItemPresenterTest.() -> Unit = {},
  val assert: suspend BaseLibraryItemPresenterTest.() -> Unit,
)

private val OnBack = EventTest(
  event = LibraryItemUiEvent.OnBack,
  assert = { navigator.awaitPop() },
)

private val PlayClick = EventTest(
  event = LibraryItemUiEvent.PlayClick(null),
  assert = {
    assertThat(analytics.events)
      .single()
      .all {
        prop(AnalyticEvent::eventName).isEqualTo("play_item_clicked")
        // No method override → no method extra on the event
        transform { it.params?.get("method") }.isNull()
      }

    assertThat(playbackController.session)
      .isInstanceOf<PlaybackControllerSession.Started>()
      .all {
        transform { it.itemId }.isEqualTo(TestLibraryItemId)
        transform { it.methodOverride }.isNull()
      }
  },
)

private val PlayClickWithMethod = EventTest(
  event = LibraryItemUiEvent.PlayClick(PlayMethod.Transcode),
  assert = {
    assertThat(analytics.events)
      .single()
      .all {
        prop(AnalyticEvent::eventName).isEqualTo("play_item_clicked")
        transform { it.params?.get("method") }.isEqualTo(PlayMethod.Transcode)
      }

    // The per-listen override travels with the session start so the router honors it
    assertThat(playbackController.session)
      .isInstanceOf<PlaybackControllerSession.Started>()
      .all {
        transform { it.itemId }.isEqualTo(TestLibraryItemId)
        transform { it.methodOverride }.isEqualTo(PlayMethod.Transcode)
      }
  },
)

private val AuthorClick = EventTest(
  event = LibraryItemUiEvent.AuthorClick(
    libraryItem {
      media {
        metadata {
          authors = listOf(
            authorMetadata(
              id = "test_author_id",
              name = "test_author_name",
            ),
          )
        }
      }
    },
    "test_author_name",
  ),
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("author_clicked")

    assertThat(navigator.awaitNextScreen())
      .isInstanceOf<AuthorDetailScreen>()
      .all {
        prop(AuthorDetailScreen::authorId)
          .isEqualTo("test_author_id")
        prop(AuthorDetailScreen::authorName)
          .isEqualTo("test_author_name")
      }
  },
)

private val NarratorClick = EventTest(
  event = LibraryItemUiEvent.NarratorClick(
    libraryItem {
      media {
        metadata {
          narratorName = "test_narrator"
        }
      }
    },
    "test_narrator",
  ),
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("narrator_clicked")

    assertThat(navigator.awaitNextScreen())
      .isInstanceOf<LibraryScreen>()
      .prop(LibraryScreen::filter)
      .isNotNull()
      .all {
        prop(ContentFilter::group)
          .isEqualTo("narrators")
        prop(ContentFilter::value)
          .isEqualTo("test_narrator")
      }
  },
)

private val SeriesClick = EventTest(
  event = LibraryItemUiEvent.SeriesClick(
    libraryItem {
      media {
        metadata {
          seriesSequence = SeriesSequence("test_series_id", "test_series_name", 0.0)
        }
      }
    },
    SeriesSequence("test_series_id", "test_series_name", 0.0),
  ),
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("series_clicked")

    assertThat(navigator.awaitNextScreen())
      .isInstanceOf<SeriesDetailScreen>()
      .all {
        prop(SeriesDetailScreen::seriesId)
          .isEqualTo("test_series_id")
        prop(SeriesDetailScreen::seriesName)
          .isEqualTo("test_series_name")
      }
  },
)

private val DiscardProgress = EventTest(
  event = LibraryItemUiEvent.DiscardProgress(libraryItem(TestLibraryItemId)),
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("discard_progress_clicked")

    assertThat(playbackController.session)
      .isInstanceOf<PlaybackControllerSession.Stopped>()
      .prop(PlaybackControllerSession.Stopped::itemId)
      .isEqualTo(TestLibraryItemId)

    assertThat(sessionsRepository.invocations)
      .firstInstanceOf<FakeSessionsRepository.Invocation.MarkDeleted>()
      .prop(FakeSessionsRepository.Invocation.MarkDeleted::libraryItemId)
      .isEqualTo(TestLibraryItemId)

    assertThat(mediaProgressRepository.invocations)
      .firstInstanceOf<FakeMediaProgressRepository.Invocation.DeleteProgress>()
      .prop(FakeMediaProgressRepository.Invocation.DeleteProgress::libraryItemId)
      .isEqualTo(TestLibraryItemId)
  },
)

private val MarkFinished = EventTest(
  event = LibraryItemUiEvent.MarkFinished(libraryItem(TestLibraryItemId)),
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("mark_finished_clicked")

    assertThat(playbackController.session)
      .isInstanceOf<PlaybackControllerSession.Stopped>()
      .prop(PlaybackControllerSession.Stopped::itemId)
      .isEqualTo(TestLibraryItemId)

    assertThat(sessionsRepository.invocations)
      .firstInstanceOf<FakeSessionsRepository.Invocation.MarkDeleted>()
      .prop(FakeSessionsRepository.Invocation.MarkDeleted::libraryItemId)
      .isEqualTo(TestLibraryItemId)

    assertThat(mediaProgressRepository.invocations)
      .firstInstanceOf<FakeMediaProgressRepository.Invocation.MarkFinished>()
      .prop(FakeMediaProgressRepository.Invocation.MarkFinished::libraryItemId)
      .isEqualTo(TestLibraryItemId)
  },
)

private val MarkNotFinished = EventTest(
  event = LibraryItemUiEvent.MarkNotFinished(libraryItem(TestLibraryItemId)),
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("mark_not_finished_clicked")

    assertThat(mediaProgressRepository.invocations)
      .firstInstanceOf<FakeMediaProgressRepository.Invocation.MarkNotFinished>()
      .prop(FakeMediaProgressRepository.Invocation.MarkNotFinished::libraryItemId)
      .isEqualTo(TestLibraryItemId)
  },
)

private val ChapterClickWithoutSessionOrPlayer = EventTest(
  event = LibraryItemUiEvent.ChapterClick(libraryItem(TestLibraryItemId), chapter(20)),
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("chapter_clicked")

    assertThat(playbackController.session)
      .isInstanceOf<PlaybackControllerSession.Started>()
      .all {
        prop(PlaybackControllerSession.Started::itemId).isEqualTo(TestLibraryItemId)
        prop(PlaybackControllerSession.Started::playImmediately).isTrue()
        prop(PlaybackControllerSession.Started::chapterId).isEqualTo(20)
      }
  },
)

private val ChapterClickWithSessionWithoutPlayer = EventTest(
  event = LibraryItemUiEvent.ChapterClick(libraryItem(TestLibraryItemId), chapter(20)),
  setup = {
    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = libraryItem(TestLibraryItemId),
    )
  },
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("chapter_clicked")

    assertThat(playbackController.session)
      .isInstanceOf<PlaybackControllerSession.Started>()
      .all {
        prop(PlaybackControllerSession.Started::itemId).isEqualTo(TestLibraryItemId)
        prop(PlaybackControllerSession.Started::playImmediately).isTrue()
        prop(PlaybackControllerSession.Started::chapterId).isEqualTo(20)
      }
  },
)

private val ChapterClickWithSessionAndPlayer = EventTest(
  event = LibraryItemUiEvent.ChapterClick(libraryItem(TestLibraryItemId), chapter(20)),
  setup = {
    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = libraryItem(TestLibraryItemId),
    )
    audioPlayerHolder.setCurrentPlayer(audioPlayer)
  },
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("chapter_clicked")

    assertThat(playbackController.session).isInstanceOf<PlaybackControllerSession.None>()
    assertThat(audioPlayer.invocations)
      .firstInstanceOf<FakeAudioPlayer.Invocation.SeekTo>()
      .prop(FakeAudioPlayer.Invocation.SeekTo::value)
      .isEqualTo(20)
  },
)

private val ChapterClickWithDifferentSessionAndPlayer = EventTest(
  event = LibraryItemUiEvent.ChapterClick(libraryItem(TestLibraryItemId), chapter(20)),
  setup = {
    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = libraryItem("some_other_id"),
    )
    audioPlayerHolder.setCurrentPlayer(audioPlayer)
  },
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("chapter_clicked")

    assertThat(playbackController.session)
      .isInstanceOf<PlaybackControllerSession.Started>()
      .all {
        prop(PlaybackControllerSession.Started::itemId).isEqualTo(TestLibraryItemId)
        prop(PlaybackControllerSession.Started::playImmediately).isTrue()
        prop(PlaybackControllerSession.Started::chapterId).isEqualTo(20)
      }
  },
)

private val DownloadClick = EventTest(
  event = LibraryItemUiEvent.DownloadClick(doNotShowAgain = true),
  setup = { libraryViewSettings.showConfirmDownload = true },
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("download_clicked")

    assertThat(libraryViewSettings.showConfirmDownload).isFalse()
    assertThat(downloads.invocations).containsExactly(
      FakeDownloads.Invocation.Download(listOf(DownloadContent.Audiobook(TestLibraryItemId))),
    )
  },
)

private val RemoveDownloadClick = EventTest(
  event = LibraryItemUiEvent.RemoveDownloadClick,
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("delete_download_clicked")

    assertThat(downloads.invocations).containsExactly(
      FakeDownloads.Invocation.Delete(DownloadContent.Audiobook(TestLibraryItemId)),
    )
  },
)

private val StopDownloadClick = EventTest(
  event = LibraryItemUiEvent.StopDownloadClick,
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("stop_download_clicked")

    assertThat(downloads.invocations).containsExactly(
      FakeDownloads.Invocation.Stop(DownloadContent.Audiobook(TestLibraryItemId)),
    )
  },
)

private val TimeInBookChange = EventTest(
  event = LibraryItemUiEvent.TimeInBookChange(enabled = true),
  setup = { libraryViewSettings.showTimeInBook = false },
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("time_in_book_clicked")

    assertThat(libraryViewSettings.showTimeInBook).isTrue()
  },
)

private val DeleteItemClickSoft = EventTest(
  event = LibraryItemUiEvent.DeleteItemClick(hardDelete = false),
  assert = {
    assertThat(libraryItemRepository.deleteInvocations)
      .single()
      .isEqualTo(TestLibraryItemId to false)

    navigator.awaitPop()
  },
)

private val EditClick = EventTest(
  event = LibraryItemUiEvent.EditClick,
  assert = {
    assertThat(navigator.awaitNextScreen()).isEqualTo(ItemEditorScreen(TestLibraryItemId))
  },
)

private val DeleteItemClickHard = EventTest(
  event = LibraryItemUiEvent.DeleteItemClick(hardDelete = true),
  assert = {
    assertThat(libraryItemRepository.deleteInvocations)
      .single()
      .isEqualTo(TestLibraryItemId to true)

    navigator.awaitPop()
  },
)
