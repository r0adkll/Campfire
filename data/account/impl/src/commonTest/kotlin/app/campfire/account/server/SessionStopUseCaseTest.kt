// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.server

import app.campfire.audioplayer.QueueOnStop
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.PlaybackControllerSession
import app.campfire.common.test.session
import app.campfire.home.ui.libraryItem
import app.campfire.sessions.test.FakeSessionsRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SessionStopUseCaseTest {

  private val sessionsRepository = FakeSessionsRepository()
  private val playbackController = FakePlaybackController()
  private val useCase = SessionStopUseCase(sessionsRepository, playbackController)

  @Test
  fun `leaving the account stops playback and keeps its queue`() = runTest {
    sessionsRepository.currentSession = session(libraryItem = libraryItem(id = "podcast"))
      .copy(episodeId = "episode")

    useCase.onDestroy()

    assertThat(playbackController.session).isEqualTo(
      PlaybackControllerSession.Stopped(
        itemId = "podcast",
        queue = QueueOnStop.Keep,
        episodeId = "episode",
      ),
    )
  }

  @Test
  fun `leaving the account with nothing playing does nothing`() = runTest {
    useCase.onDestroy()

    assertThat(playbackController.session).isEqualTo(PlaybackControllerSession.None)
  }
}
