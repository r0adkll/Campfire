// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.common.test.session
import app.campfire.home.ui.libraryItem
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class LoadedItemTest {

  private val player = FakeAudioPlayer()

  @Test
  fun `a paused player with the item can be controlled`() {
    player.load("book", AudioPlayer.State.Paused)

    assertThat(player.hasLoaded("book")).isTrue()
  }

  @Test
  fun `another item loaded isn't this one`() {
    player.load("other", AudioPlayer.State.Playing)

    assertThat(player.hasLoaded("book")).isFalse()
  }

  @Test
  fun `a player that needs preparing again hasn't the item loaded`() {
    for (state in listOf(AudioPlayer.State.Disabled, AudioPlayer.State.Initializing, AudioPlayer.State.Finished)) {
      player.load("book", state)

      assertThat(player.hasLoaded("book")).isFalse()
    }
  }

  @Test
  fun `nothing prepared has nothing loaded`() {
    player.state.value = AudioPlayer.State.Playing

    assertThat(player.hasLoaded("book")).isFalse()
  }

  @Test
  fun `an episode must be the one loaded`() {
    player.load("podcast", AudioPlayer.State.Playing, episodeId = "episode-1")

    assertThat(player.hasLoaded("podcast", episodeId = "episode-1")).isTrue()
    assertThat(player.hasLoaded("podcast", episodeId = "episode-2")).isFalse()
    assertThat(player.hasLoaded("podcast")).isTrue()
  }

  @Test
  fun `the loaded player is the current one only while it has the item`() {
    val holder = FakeAudioPlayerHolder()
    assertThat(holder.loadedPlayerFor("book")).isNull()

    player.load("book", AudioPlayer.State.Paused)
    holder.setCurrentPlayer(player)
    assertThat(holder.loadedPlayerFor("book")).isSameInstanceAs(player)
    assertThat(holder.loadedPlayerFor("other")).isNull()

    player.state.value = AudioPlayer.State.Disabled
    assertThat(holder.loadedPlayerFor("book")).isNull()
  }

  @Test
  fun `observing follows the player and its state`() = runTest {
    val holder = FakeAudioPlayerHolder()

    holder.observeHasLoaded("book").test {
      assertThat(awaitItem()).isFalse()

      player.load("book", AudioPlayer.State.Initializing)
      holder.setCurrentPlayer(player)
      expectNoEvents()

      player.state.value = AudioPlayer.State.Paused
      assertThat(awaitItem()).isTrue()

      player.state.value = AudioPlayer.State.Finished
      assertThat(awaitItem()).isFalse()

      player.state.value = AudioPlayer.State.Playing
      assertThat(awaitItem()).isTrue()

      holder.setCurrentPlayer(null)
      assertThat(awaitItem()).isFalse()
    }
  }

  private fun FakeAudioPlayer.load(
    libraryItemId: String,
    state: AudioPlayer.State,
    episodeId: String? = null,
  ) {
    preparedSession = session(libraryItem = libraryItem(id = libraryItemId)).copy(episodeId = episodeId)
    this.state.value = state
  }
}
