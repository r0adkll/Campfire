// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.common.test.mediaProgress
import app.campfire.common.test.session
import app.campfire.home.ui.libraryItem
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest

class LivePositionTest {

  @Test
  fun `a book's progress takes the live position`() {
    val progress =
      mediaProgress(libraryItemId = "book", episodeId = "", duration = 100f, currentTime = 10f, progress = 0.1f)

    val live = progress.withLivePosition(LivePosition("book", episodeId = null, time = 25.seconds))

    assertThat(live.currentTime).isEqualTo(25f)
    assertThat(live.progress).isEqualTo(0.25f)
  }

  @Test
  fun `an episode's progress takes the live position for that episode only`() {
    val progress = mediaProgress(libraryItemId = "show", episodeId = "ep1", duration = 200f, currentTime = 10f)

    assertThat(progress.withLivePosition(LivePosition("show", "ep1", 50.seconds)).currentTime).isEqualTo(50f)
    assertThat(progress.withLivePosition(LivePosition("show", "ep2", 50.seconds))).isSameInstanceAs(progress)
  }

  @Test
  fun `another item's progress is left alone`() {
    val progress = mediaProgress(libraryItemId = "book", currentTime = 10f)

    assertThat(progress.withLivePosition(LivePosition("other", null, 50.seconds))).isSameInstanceAs(progress)
    assertThat(progress.withLivePosition(null)).isSameInstanceAs(progress)
  }

  @Test
  fun `finished progress is left alone`() {
    val progress = mediaProgress(libraryItemId = "book", currentTime = 100f, isFinished = true)

    assertThat(progress.withLivePosition(LivePosition("book", null, 5.seconds))).isSameInstanceAs(progress)
  }

  @Test
  fun `progress without a duration keeps its fraction`() {
    val progress = mediaProgress(libraryItemId = "book", progress = 0.4f).copy(duration = null)

    val live = progress.withLivePosition(LivePosition("book", null, 25.seconds))

    assertThat(live.currentTime).isEqualTo(25f)
    assertThat(live.progress).isEqualTo(0.4f)
  }

  @Test
  fun `the live position follows the prepared player in whole seconds`() = runTest {
    val holder = FakeAudioPlayerHolder()
    val player = FakeAudioPlayer().apply {
      preparedSession = session(libraryItem = libraryItem(id = "book"))
    }

    holder.observeLivePosition().test {
      // No player
      assertThat(awaitItem()).isNull()

      // Prepared, but no position of its own yet
      holder.setCurrentPlayer(player)
      expectNoEvents()

      player.overallTime.value = 12_400.milliseconds
      player.state.value = AudioPlayer.State.Playing
      assertThat(awaitItem()).isEqualTo(LivePosition("book", null, 12.seconds))

      // Ticks inside the same second don't emit
      player.overallTime.value = 12_900.milliseconds
      expectNoEvents()

      player.overallTime.value = 13_100.milliseconds
      assertThat(awaitItem()).isEqualTo(LivePosition("book", null, 13.seconds))

      holder.setCurrentPlayer(null)
      assertThat(awaitItem()).isNull()
    }
  }
}
