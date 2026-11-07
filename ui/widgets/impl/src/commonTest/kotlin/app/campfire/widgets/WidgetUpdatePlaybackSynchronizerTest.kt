// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets

import app.campfire.audioplayer.AudioPlayer
import app.campfire.core.time.FatherTime
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class WidgetUpdatePlaybackSynchronizerTest {

  private val clock = FakeFatherTime()
  private val updater = RecordingWidgetUpdater()
  private val synchronizer = WidgetUpdatePlaybackSynchronizer(updater, NoOpPinRequester, clock)

  /** Plays from [from] for [length], reporting the position every half second as the player does. */
  private suspend fun play(from: Duration, length: Duration) {
    var time = from
    while (time <= from + length) {
      synchronizer.onCurrentTimeChanged(ITEM, time)
      clock.advance(500.milliseconds)
      time += 500.milliseconds
    }
  }

  @Test
  fun `the position is only sent when what the widget shows changes`() = runTest {
    // A 40 minute chapter: 1% of it is 24 seconds, and the readout reads in whole minutes. From
    // 1m10s to 1m22s it reads "38m" with the bar at 3% throughout.
    synchronizer.onCurrentDurationChanged(ITEM, 40.minutes)
    updater.updates.clear()
    clock.advance(10.seconds)

    play(from = 70.seconds, length = 12.seconds)

    // The first tick sends the position, then the widget wouldn't change for the other 24
    assertThat(updater.updates).containsExactly(Update(currentTime = 70.seconds))
  }

  @Test
  fun `a change to the readout is sent, but no more than once every five seconds`() = runTest {
    // A 1 minute chapter: the readout counts seconds and moves on every tick
    synchronizer.onCurrentDurationChanged(ITEM, 1.minutes)
    updater.updates.clear()
    clock.advance(10.seconds)

    play(from = 0.seconds, length = 10.seconds)

    assertThat(updater.updates.map { it.currentTime }).containsExactly(0.seconds, 5.seconds, 10.seconds)
  }

  @Test
  fun `a new chapter is sent along with the position`() = runTest {
    synchronizer.onCurrentTimeChanged(ITEM, 30.seconds)
    updater.updates.clear()

    synchronizer.onCurrentDurationChanged(ITEM, 12.minutes)

    assertThat(updater.updates).containsExactly(Update(currentTime = 30.seconds, currentDuration = 12.minutes))
  }

  @Test
  fun `pausing sends the latest position`() = runTest {
    synchronizer.onCurrentDurationChanged(ITEM, 1.minutes)
    clock.advance(10.seconds)
    play(from = 0.seconds, length = 2.seconds)
    updater.updates.clear()

    synchronizer.onStateChanged(Uuid.random(), ITEM, AudioPlayer.State.Paused, AudioPlayer.State.Playing)

    assertThat(updater.updates).containsExactly(Update(currentTime = 2.seconds))
  }

  @Test
  fun `a state change before any position leaves the widget's position alone`() = runTest {
    synchronizer.onStateChanged(Uuid.random(), ITEM, AudioPlayer.State.Playing, AudioPlayer.State.Paused)

    assertThat(updater.updates.single().currentTime).isNull()
  }

  @Test
  fun `the readout matches the widget's remaining time and progress`() {
    val readout = WidgetTimeReadout.of(
      currentTime = 3.minutes,
      currentDuration = 10.minutes,
      playbackSpeed = 2f,
    )

    assertThat(readout).isEqualTo(WidgetTimeReadout(remaining = "3m ", progressPercent = 30))
  }

  private companion object {
    const val ITEM = "li_1"
  }
}

private data class Update(
  val currentTime: Duration? = null,
  val currentDuration: Duration? = null,
  val playbackSpeed: Float? = null,
)

private class RecordingWidgetUpdater : WidgetUpdater {
  val updates = mutableListOf<Update>()

  override suspend fun updatePlayerWidget(currentTime: Duration?, currentDuration: Duration?, playbackSpeed: Float?) {
    updates += Update(currentTime, currentDuration, playbackSpeed)
  }
}

private object NoOpPinRequester : WidgetPinRequester {
  override fun requestPinWidget() = Unit
}

private class FakeFatherTime : FatherTime {
  private var millis = 0L

  fun advance(by: Duration) {
    millis += by.inWholeMilliseconds
  }

  override fun nowInEpochMillis(): Long = millis
  override fun now(): LocalDateTime = error("unused")
  override fun today(): LocalDate = error("unused")
}
