package app.campfire.libraries.ui.detail

import app.campfire.analytics.events.AnalyticEvent
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.PlaybackControllerSession
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.test.assert.firstInstanceOf
import app.campfire.common.test.session
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.SeriesSequence
import app.campfire.home.ui.authorMetadata
import app.campfire.home.ui.chapter
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.user.test.FakeMediaProgressRepository
import app.cash.burst.Burst
import app.cash.burst.burstValues
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.single
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

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
      OnBack,
    ),
  ) = runTest {
    val libraryItem = emptyLibraryItem()
    libraryItemRepository.libraryItemFlow.emit(libraryItem)

    eventTest.setup(this@LibraryItemPresenterEventsTest)

    presenter.test {
      skipItems(1)
      val item = awaitItem()

      item.eventSink(eventTest.event)

      eventTest.assert(this@LibraryItemPresenterEventsTest)

      cancelAndIgnoreRemainingEvents()
    }
  }
}

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
  event = LibraryItemUiEvent.PlayClick(libraryItem(TestLibraryItemId)),
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("play_item_clicked")

    assertThat(playbackController.session)
      .isInstanceOf<PlaybackControllerSession.Started>()
      .transform { it.itemId }
      .isEqualTo(TestLibraryItemId)
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
          seriesSequence = SeriesSequence("test_series_id", "test_series_name", 0)
        }
      }
    },
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
      .firstInstanceOf<FakeSessionsRepository.Invocation.DeleteSession>()
      .prop(FakeSessionsRepository.Invocation.DeleteSession::libraryItemId)
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
      .firstInstanceOf<FakeSessionsRepository.Invocation.DeleteSession>()
      .prop(FakeSessionsRepository.Invocation.DeleteSession::libraryItemId)
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
  setup = { settings.showConfirmDownload = true },
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("download_clicked")

    assertThat(settings.showConfirmDownload).isFalse()
    assertThat(offlineDownloadManager.invocations)
      .firstInstanceOf<FakeOfflineDownloadManager.Invocation.Download>()
      .transform { it.item.id }
      .isEqualTo(TestLibraryItemId)
  },
)

private val RemoveDownloadClick = EventTest(
  event = LibraryItemUiEvent.RemoveDownloadClick,
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("delete_download_clicked")

    assertThat(offlineDownloadManager.invocations)
      .firstInstanceOf<FakeOfflineDownloadManager.Invocation.Delete>()
      .transform { it.item.id }
      .isEqualTo(TestLibraryItemId)
  },
)

private val StopDownloadClick = EventTest(
  event = LibraryItemUiEvent.StopDownloadClick,
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("stop_download_clicked")

    assertThat(offlineDownloadManager.invocations)
      .firstInstanceOf<FakeOfflineDownloadManager.Invocation.Stop>()
      .transform { it.item.id }
      .isEqualTo(TestLibraryItemId)
  },
)

private val TimeInBookChange = EventTest(
  event = LibraryItemUiEvent.TimeInBookChange(enabled = true),
  setup = { settings.showTimeInBook = false },
  assert = {
    assertThat(analytics.events)
      .single()
      .prop(AnalyticEvent::eventName)
      .isEqualTo("time_in_book_clicked")

    assertThat(settings.showTimeInBook).isTrue()
  },
)
