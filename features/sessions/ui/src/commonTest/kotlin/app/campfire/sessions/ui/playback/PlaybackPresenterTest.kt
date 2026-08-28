// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback

import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.common.test.session
import app.campfire.core.model.preview.libraryItem
import app.campfire.libraries.test.FakeLibraryItemValidator
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.test.FakePlaybackSettings
import app.campfire.settings.test.TestThemeSettings
import app.campfire.ui.theming.test.FakeThemeManager
import app.campfire.user.test.FakeMediaProgressRepository
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest

class PlaybackPresenterTest {

  private val sessionQueue = FakeSessionQueue()
  private val sessionsRepository = FakeSessionsRepository()
  private val libraryItemValidator = FakeLibraryItemValidator()
  private val mediaProgressRepository = FakeMediaProgressRepository()
  private val playbackController = FakePlaybackController()
  private val playbackSettings = FakePlaybackSettings()
  private val audioPlayerHolder = FakeAudioPlayerHolder()
  private val themeSettings = TestThemeSettings()
  private val themeManager = FakeThemeManager()

  private val presenter = PlaybackPresenter(
    sessionQueue = sessionQueue,
    sessionsRepository = sessionsRepository,
    libraryItemValidator = libraryItemValidator,
    mediaProgressRepository = mediaProgressRepository,
    playbackController = playbackController,
    playbackSettings = playbackSettings,
    audioPlayerHolder = audioPlayerHolder,
    themeSettings = themeSettings,
    themeManager = themeManager,
  )

  @Test
  fun `player state shows session-derived timing while no player is prepared`() = runTest {
    // 10 hours across 10 chapters; 90 minutes in = 30 minutes into "Chapter 2"
    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = libraryItem(duration = 10.hours, numOfChapters = 10),
      currentTime = 90.minutes,
    )

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.playerState.duration > 0.minutes }
      assertThat(state.playerState.time).isEqualTo(30.minutes)
      assertThat(state.playerState.bookTime).isEqualTo(90.minutes)
      assertThat(state.playerState.duration).isEqualTo(1.hours)
      assertThat(state.playerState.metadata.title).isEqualTo("Chapter 2")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `placeholder persists while a present player is still disabled`() = runTest {
    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = libraryItem(duration = 10.hours, numOfChapters = 10),
      currentTime = 90.minutes,
    )
    // A player exists (service came up) but hasn't prepared this session yet — its flows
    // are all at defaults and its state is Disabled
    audioPlayerHolder.setCurrentPlayer(FakeAudioPlayer())

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.playerState.duration > 0.minutes }
      assertThat(state.playerState.time).isEqualTo(30.minutes)
      assertThat(state.playerState.metadata.title).isEqualTo("Chapter 2")
      cancelAndIgnoreRemainingEvents()
    }
  }

  private suspend fun app.cash.turbine.ReceiveTurbine<PlaybackUiState>.awaitItemMatching(
    predicate: (PlaybackUiState) -> Boolean,
  ): PlaybackUiState {
    while (true) {
      val item = awaitItem()
      if (predicate(item)) return item
    }
  }
}
