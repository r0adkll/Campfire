// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.common.test.assert.firstInstanceOf
import app.campfire.common.test.session
import app.campfire.home.ui.libraryItem
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest

class ItemPlaybackHistoryTest {

  private val playbackHistoryUi = FakePlaybackHistoryUi()
  private val player = FakeAudioPlayer()
  private val audioPlayerHolder = FakeAudioPlayerHolder()
  private val playbackHistory = ItemPlaybackHistory(playbackHistoryUi, audioPlayerHolder)

  @Test
  fun aMomentPicked_whileTheBookIsLoaded_isJumpedTo() = runTest {
    load(BOOK_ID)
    playbackHistoryUi.picks = 12.minutes

    playbackHistory.show(NoOverlayHost, BOOK_ID)

    assertThat(playbackHistoryUi.shown).containsExactly(BOOK_ID)
    assertThat(player.invocations)
      .firstInstanceOf<FakeAudioPlayer.Invocation.SeekTo>()
      .prop(FakeAudioPlayer.Invocation.SeekTo::value)
      .isEqualTo(12.minutes)
  }

  @Test
  fun nothingPicked_leavesThePlayerAlone() = runTest {
    load(BOOK_ID)

    playbackHistory.show(NoOverlayHost, BOOK_ID)

    assertThat(player.invocations).isEmpty()
  }

  @Test
  fun anotherBookLoaded_isntJumped() = runTest {
    load("other_book")
    playbackHistoryUi.picks = 12.minutes

    playbackHistory.show(NoOverlayHost, BOOK_ID)

    assertThat(player.invocations).isEmpty()
  }

  @Test
  fun aBookWhosePlayerNeedsPreparing_isntJumped() = runTest {
    // Its session is still the active one after the app was killed, but nothing is loaded
    load(BOOK_ID, AudioPlayer.State.Disabled)
    playbackHistoryUi.picks = 12.minutes

    playbackHistory.show(NoOverlayHost, BOOK_ID)

    assertThat(player.invocations).isEmpty()
  }

  @Test
  fun aBookLetGoAsTheSheetCloses_isntJumped() = runTest {
    load(BOOK_ID)
    playbackHistoryUi.picks = 12.minutes
    playbackHistoryUi.onClose = { audioPlayerHolder.setCurrentPlayer(null) }

    playbackHistory.show(NoOverlayHost, BOOK_ID)

    assertThat(player.invocations).isEmpty()
  }

  private fun load(libraryItemId: String, state: AudioPlayer.State = AudioPlayer.State.Paused) {
    player.preparedSession = session(libraryItem = libraryItem(id = libraryItemId))
    player.state.value = state
    audioPlayerHolder.setCurrentPlayer(player)
  }

  private companion object {
    const val BOOK_ID = "book"
  }
}
