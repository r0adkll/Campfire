// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.queue

import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.PlaybackControllerSession
import app.campfire.common.screens.QueueScreen
import app.campfire.common.test.session
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.sessions.api.QueuedEntry
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.sessions.ui.playback.QueueUiEvent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class QueuePresenterTest {

  private val navigator = FakeNavigator(QueueScreen)
  private val sessionQueue = FakeSessionQueue()
  private val sessionsRepository = FakeSessionsRepository()
  private val playbackController = FakePlaybackController()

  private val playing = libraryItem(id = "playing")
  private val first = libraryItem(id = "first")
  private val second = libraryItem(id = "second")

  private val presenter = QueuePresenter(
    navigator = navigator,
    sessionQueue = sessionQueue,
    sessionsRepository = sessionsRepository,
    playbackController = playbackController,
  )

  @Test
  fun `shows what's playing above the queue`() = runTest {
    sessionQueue.add(first)
    sessionsRepository.currentSessionFlow.value = session(libraryItem = playing)

    presenter.test {
      val state = expectMostRecentItem()
      assertThat(state.nowPlaying).isEqualTo(QueuedEntry(playing))
      assertThat(state.queueState.queue.map { it.key }).containsExactly("first")
    }
  }

  @Test
  fun `with nothing playing there's nothing above the queue`() = runTest {
    sessionQueue.add(first)

    presenter.test {
      assertThat(expectMostRecentItem().nowPlaying).isNull()
    }
  }

  @Test
  fun `playing the queue starts its first entry`() = runTest {
    sessionQueue.add(first)
    sessionQueue.add(second)

    presenter.test {
      expectMostRecentItem().eventSink(QueueScreenUiEvent.PlayQueue)

      assertThat(playbackController.session).isEqualTo(
        PlaybackControllerSession.Started(
          itemId = "first",
          playImmediately = true,
          chapterId = null,
          episodeId = null,
        ),
      )
      testScheduler.advanceUntilIdle()
      cancelAndIgnoreRemainingEvents()
    }
    assertThat(sessionQueue.queue.map { it.key }).containsExactly("second")
  }

  @Test
  fun `playing an empty queue does nothing`() = runTest {
    presenter.test {
      expectMostRecentItem().eventSink(QueueScreenUiEvent.PlayQueue)

      assertThat(playbackController.session).isEqualTo(PlaybackControllerSession.None)
    }
  }

  @Test
  fun `opening an entry goes to its page`() = runTest {
    sessionQueue.add(first)

    presenter.test {
      val state = expectMostRecentItem()
      state.eventSink(QueueScreenUiEvent.OpenItem(state.queueState.queue.single()))

      assertThat(navigator.awaitNextScreen()).isEqualTo(LibraryItemScreen(libraryItemId = "first"))
    }
  }

  @Test
  fun `back leaves the screen`() = runTest {
    presenter.test {
      expectMostRecentItem().eventSink(QueueScreenUiEvent.Back)

      navigator.awaitPop()
    }
  }

  @Test
  fun `dragging an entry saves the new order when the drag stops`() = runTest {
    sessionQueue.add(first)
    sessionQueue.add(second)

    presenter.test {
      val state = expectMostRecentItem()
      state.queueState.eventSink(QueueUiEvent.ReorderItem(fromKey = "second", toKey = "first"))
      state.queueState.eventSink(QueueUiEvent.ReorderStopped)
      testScheduler.advanceUntilIdle()
      cancelAndIgnoreRemainingEvents()
    }
    assertThat(sessionQueue.queue.map { it.key }).containsExactly("second", "first")
  }

  @Test
  fun `clearing empties the queue`() = runTest {
    sessionQueue.add(first)

    presenter.test {
      expectMostRecentItem().queueState.eventSink(QueueUiEvent.ClearQueue)
      testScheduler.advanceUntilIdle()
      cancelAndIgnoreRemainingEvents()
    }
    assertThat(sessionQueue.queue).isEmpty()
  }
}
