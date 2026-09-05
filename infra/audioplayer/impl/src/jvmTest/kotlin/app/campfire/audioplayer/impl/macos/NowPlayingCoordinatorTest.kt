// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalCoroutinesApi::class)

package app.campfire.audioplayer.impl.macos

import app.campfire.audioplayer.AudioPlayer.State
import app.campfire.audioplayer.impl.fixtures.chapter
import app.campfire.audioplayer.impl.fixtures.session
import app.campfire.audioplayer.impl.fixtures.track
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayer.Invocation
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.settings.test.FakePlaybackSettings
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource

class NowPlayingCoordinatorTest {

  private class FakeBridge : NowPlayingBridge {
    val infos = mutableListOf<NowPlayingInfo?>()
    val states = mutableListOf<NowPlayingState>()
    var handler: RemoteCommandHandler? = null
    var skipForward: Duration? = null
    var skipBackward: Duration? = null

    override fun setNowPlaying(info: NowPlayingInfo?) {
      infos += info
    }

    override fun setPlaybackState(state: NowPlayingState) {
      states += state
    }

    override fun setCommandHandler(handler: RemoteCommandHandler?, skipForward: Duration, skipBackward: Duration) {
      this.handler = handler
      this.skipForward = skipForward
      this.skipBackward = skipBackward
    }
  }

  private val bridge = FakeBridge()
  private val holder = FakeAudioPlayerHolder()
  private val settings = FakePlaybackSettings()

  private fun TestScope.start(): Job {
    val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
    return NowPlayingCoordinator(holder, settings, bridge, scope, testTimeSource).start()
  }

  /** A player mid-way through chapter 1 of a two-track book, playing at 1.25x. */
  private fun playingPlayer() = FakeAudioPlayer().apply {
    preparedSession = session(
      chapters = listOf(chapter(0, 0f, 600f), chapter(1, 600f, 1800f)),
      tracks = listOf(track(1, 0f, 1800f)),
    )
    currentMetadata.value = Metadata(title = "Chapter 1")
    currentDuration.value = 10.minutes
    playbackSpeed.value = 1.25f
    currentTime.value = 5.seconds
    state.value = State.Playing
  }

  @Test
  fun `without a player the system entry is cleared and commands are unregistered`() = runTest {
    start()
    assertThat(bridge.handler).isNull()
    assertThat(bridge.infos).containsExactly(null)
    assertThat(bridge.states).containsExactly(NowPlayingState.Stopped)
  }

  @Test
  fun `a player publishes metadata, session details, rate, and transport state`() = runTest {
    start()
    holder.setCurrentPlayer(playingPlayer())

    assertThat(bridge.handler).isNotNull()
    assertThat(bridge.skipForward).isEqualTo(30.seconds)
    assertThat(bridge.skipBackward).isEqualTo(10.seconds)
    assertThat(bridge.infos.last()).isEqualTo(
      NowPlayingInfo(
        title = "Chapter 1",
        artist = "An Author",
        album = "A Book",
        duration = 10.minutes,
        elapsed = 5.seconds,
        rate = 1.25,
        defaultRate = 1.25,
      ),
    )
    assertThat(bridge.states.last()).isEqualTo(NowPlayingState.Playing)
  }

  @Test
  fun `elapsed time is republished only when it drifts from the extrapolated position`() = runTest {
    start()
    val player = playingPlayer()
    holder.setCurrentPlayer(player)
    val published = bridge.infos.size

    // 4s of wall time at 1.25x puts the expected position at 10s; a tick there is not news
    advanceTimeBy(4_000)
    player.currentTime.value = 10.seconds
    assertThat(bridge.infos).hasSize(published)

    // A jump is a seek: republish with the new elapsed time
    player.currentTime.value = 3.minutes
    assertThat(bridge.infos).hasSize(published + 1)
    assertThat(bridge.infos.last()?.elapsed).isEqualTo(3.minutes)
  }

  @Test
  fun `pausing publishes a zero rate and the paused transport state`() = runTest {
    start()
    val player = playingPlayer()
    holder.setCurrentPlayer(player)

    player.state.value = State.Paused
    assertThat(bridge.infos.last()?.rate).isEqualTo(0.0)
    assertThat(bridge.infos.last()?.defaultRate).isEqualTo(1.25)
    assertThat(bridge.states.last()).isEqualTo(NowPlayingState.Paused)

    player.state.value = State.Buffering
    assertThat(bridge.states.last()).isEqualTo(NowPlayingState.Paused)
  }

  @Test
  fun `a disabled player clears the entry once and a removed player unregisters commands`() = runTest {
    start()
    val player = playingPlayer()
    holder.setCurrentPlayer(player)

    player.state.value = State.Disabled
    assertThat(bridge.infos.last()).isNull()
    assertThat(bridge.states.last()).isEqualTo(NowPlayingState.Stopped)
    val cleared = bridge.infos.size
    player.currentTime.value = 6.seconds
    assertThat(bridge.infos).hasSize(cleared)

    holder.setCurrentPlayer(null)
    assertThat(bridge.handler).isNull()
  }

  @Test
  fun `remote commands drive the player with chapter-relative seeks`() = runTest {
    start()
    val player = playingPlayer()
    holder.setCurrentPlayer(player)
    val commands = bridge.handler!!

    commands.play()
    assertThat(player.invocations).isEmpty()
    commands.pause()
    assertThat(player.invocations).containsExactly(Invocation.Pause)

    player.state.value = State.Paused
    commands.play()
    assertThat(player.invocations.last()).isEqualTo(Invocation.PlayPause)

    commands.togglePlayPause()
    commands.skipForward()
    commands.skipBackward()
    commands.nextTrack()
    commands.previousTrack()
    commands.seekTo(2.5.minutes)
    assertThat(player.invocations.drop(2)).containsExactly(
      Invocation.PlayPause,
      Invocation.SeekForward,
      Invocation.SeekBackward,
      Invocation.SkipToNext,
      Invocation.SkipToPrevious,
      Invocation.SeekTo(0.25f),
    )
  }

  @Test
  fun `the coordinator stops with its scope`() = runTest {
    val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
    NowPlayingCoordinator(holder, settings, bridge, scope, testTimeSource).start()
    scope.cancel()
    holder.setCurrentPlayer(playingPlayer())
    assertThat(bridge.handler).isNull()
  }
}
