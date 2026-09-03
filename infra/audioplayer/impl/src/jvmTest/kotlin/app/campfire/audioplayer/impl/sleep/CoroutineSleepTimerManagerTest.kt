// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalCoroutinesApi::class)

package app.campfire.audioplayer.impl.sleep

import app.campfire.audioplayer.AudioPlayer.State
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayer.Invocation
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.time.FatherTime
import app.campfire.settings.test.FakeSleepSettings
import app.campfire.shake.ShakeDetector
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class CoroutineSleepTimerManagerTest {

  private val settings = FakeSleepSettings().apply {
    fadeOutDuration = 5.seconds
  }
  private val player = FakeAudioPlayer()

  private fun TestScope.manager(): CoroutineSleepTimerManager {
    val dispatcher = UnconfinedTestDispatcher(testScheduler)
    return CoroutineSleepTimerManager(
      player = player,
      sleepSettings = settings,
      shakeDetector = ShakeDetector(),
      dispatcherProvider = DispatcherProvider(
        io = dispatcher,
        databaseWrite = dispatcher,
        databaseRead = dispatcher,
        computation = dispatcher,
        main = dispatcher,
      ),
      fatherTime = SchedulerFatherTime(this),
      applicationScope = backgroundScope,
    )
  }

  private fun fades() = player.invocations.filterIsInstance<Invocation.FadeToPause>()

  @Test
  fun `timer set while playing counts down and fades out with the configured duration`() = runTest {
    val manager = manager()
    player.state.value = State.Playing

    manager.setTimer(PlaybackTimer.Epoch(10_000L))
    assertThat(manager.runningTimer.value?.isPaused).isEqualTo(false)
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(10_000L)

    advanceTimeBy(9_999L)
    assertThat(fades()).isEmpty()

    advanceTimeBy(2L)
    assertThat(fades().single().duration).isEqualTo(5.seconds)
    assertThat(manager.runningTimer.value).isNull()
  }

  @Test
  fun `pausing playback freezes the countdown and resuming picks it back up`() = runTest {
    val manager = manager()
    player.state.value = State.Playing
    manager.setTimer(PlaybackTimer.Epoch(10_000L))

    advanceTimeBy(4_000L)
    player.state.value = State.Paused

    val paused = manager.runningTimer.value
    assertThat(paused?.isPaused).isEqualTo(true)
    assertThat(paused?.remainingMillis(testScheduler.currentTime)).isEqualTo(6_000L)

    // Nothing happens no matter how long playback stays paused, and the readout stays frozen
    advanceTimeBy(10.minutes.inWholeMilliseconds)
    assertThat(fades()).isEmpty()
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(6_000L)

    player.state.value = State.Playing
    assertThat(manager.runningTimer.value?.isPaused).isEqualTo(false)
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(6_000L)

    advanceTimeBy(5_999L)
    assertThat(fades()).isEmpty()
    advanceTimeBy(2L)
    assertThat(fades()).hasSize(1)
  }

  @Test
  fun `timer set while paused does not start until playback begins`() = runTest {
    val manager = manager()
    player.state.value = State.Paused

    manager.setTimer(PlaybackTimer.Epoch(10_000L))
    assertThat(manager.runningTimer.value?.isPaused).isEqualTo(true)

    advanceTimeBy(60_000L)
    assertThat(fades()).isEmpty()

    player.state.value = State.Playing
    advanceTimeBy(10_001L)
    assertThat(fades()).hasSize(1)
  }

  @Test
  fun `resuming playback mid-fade cancels the fade`() = runTest {
    val manager = manager()
    player.state.value = State.Playing
    manager.setTimer(PlaybackTimer.Epoch(1_000L))
    advanceTimeBy(1_001L)
    val fade = player.fadeJobs.single()
    assertThat(fade.isActive).isTrue()

    // A buffering blip while still fading is not a resume
    player.state.value = State.Buffering
    player.state.value = State.Playing
    assertThat(fade.isActive).isTrue()

    // The listener pauses, then presses play before the fade is done
    player.state.value = State.Paused
    player.state.value = State.Playing
    assertThat(fade.isCancelled).isTrue()
  }

  @Test
  fun `setting a new timer or clearing it mid-fade cancels the fade`() = runTest {
    val manager = manager()
    player.state.value = State.Playing

    manager.setTimer(PlaybackTimer.Epoch(1_000L))
    advanceTimeBy(1_001L)
    manager.setTimer(PlaybackTimer.Epoch(5_000L))
    assertThat(player.fadeJobs[0].isCancelled).isTrue()
    assertThat(manager.runningTimer.value).isNotNull()

    advanceTimeBy(5_001L)
    manager.clearTimer()
    assertThat(player.fadeJobs[1].isCancelled).isTrue()
  }

  @Test
  fun `auto rewind applies once the fade of an auto sleep timer completes`() = runTest {
    settings.autoSleepTimerEnabled = true
    settings.autoRewindEnabled = true
    settings.autoRewindAmount = 5.minutes
    val manager = manager()
    player.state.value = State.Playing
    player.overallTime.value = 30.minutes

    manager.setTimer(PlaybackTimer.Epoch(1_000L, isAutoSleepTimer = true))
    advanceTimeBy(1_001L)
    assertThat(player.invocations.filterIsInstance<Invocation.SeekTo>()).isEmpty()

    player.fadeJobs.single().complete()
    val seek = player.invocations.filterIsInstance<Invocation.SeekTo>().single()
    assertThat(seek.value).isEqualTo(25.minutes)
  }

  @Test
  fun `auto rewind is skipped when the fade is cancelled`() = runTest {
    settings.autoSleepTimerEnabled = true
    settings.autoRewindEnabled = true
    val manager = manager()
    player.state.value = State.Playing

    manager.setTimer(PlaybackTimer.Epoch(1_000L, isAutoSleepTimer = true))
    advanceTimeBy(1_001L)
    player.fadeJobs.single().cancel()

    assertThat(player.invocations.filterIsInstance<Invocation.SeekTo>()).isEmpty()
  }

  @Test
  fun `end of chapter timer pauses immediately at the chapter boundary`() = runTest {
    val manager = manager()
    player.state.value = State.Playing

    manager.setTimer(PlaybackTimer.EndOfChapter())
    assertThat(manager.runningTimer.value?.timer).isNotNull().isInstanceOf(PlaybackTimer.EndOfChapter::class)
    assertThat(manager.runningTimer.value?.remainingMillis(0L)).isNull()

    assertThat(manager.endOfChapter()).isTrue()
    assertThat(player.invocations.last()).isEqualTo(Invocation.Pause)
    assertThat(fades()).isEmpty()
    assertThat(manager.runningTimer.value).isNull()
    assertThat(manager.endOfChapter()).isFalse()
  }

  private class SchedulerFatherTime(private val scope: TestScope) : FatherTime {
    override fun now(): LocalDateTime = error("not used in tests")
    override fun today(): LocalDate = error("not used in tests")
    override fun nowInEpochMillis(): Long = scope.testScheduler.currentTime
  }
}
