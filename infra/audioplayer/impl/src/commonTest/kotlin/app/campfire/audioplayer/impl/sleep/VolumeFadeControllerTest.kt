// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalCoroutinesApi::class)

package app.campfire.audioplayer.impl.sleep

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

class VolumeFadeControllerTest {

  @Test
  fun gainAt_startsAtUnityAndEndsAtFloor() {
    assertThat(VolumeFadeController.gainAt(0f)).isEqualTo(1f)
    // -40 dB == 0.01 linear gain
    assertThat(VolumeFadeController.gainAt(1f)).isBetween(0.0099f, 0.0101f)
  }

  @Test
  fun gainAt_halvesPerceivedLoudnessEveryQuarter() {
    // Each quarter of the fade drops another 10 dB, i.e. a further 1/sqrt(10) in linear gain
    val quarter = VolumeFadeController.gainAt(0.25f)
    val half = VolumeFadeController.gainAt(0.5f)
    val threeQuarter = VolumeFadeController.gainAt(0.75f)
    assertThat(quarter).isBetween(0.315f, 0.317f)
    assertThat(half).isBetween(0.099f, 0.101f)
    assertThat(threeQuarter).isBetween(0.0315f, 0.0317f)
  }

  @Test
  fun gainAt_clampsOutOfRangeProgress() {
    assertThat(VolumeFadeController.gainAt(-1f)).isEqualTo(VolumeFadeController.gainAt(0f))
    assertThat(VolumeFadeController.gainAt(2f)).isEqualTo(VolumeFadeController.gainAt(1f))
  }

  @Test
  fun fade_followsLogarithmicCurveThenPausesAndRestoresVolume() = runTest {
    val player = FakeVolumePlayer(volume = 0.8f)

    val job = VolumeFadeController.fade(
      scope = this,
      duration = 1.seconds,
      tickRate = 10,
      getVolume = { player.volume },
      setVolume = { player.volume = it },
      onPause = {
        player.pauseCount++
        player.volumeWhenPaused = player.volume
      },
      now = { testScheduler.currentTime },
    )

    advanceTimeBy(501)
    // Half way through, the gain should sit at -20 dB (0.1) of the starting volume
    assertThat(player.volume).isBetween(0.079f, 0.081f)
    assertThat(player.pauseCount).isEqualTo(0)

    advanceTimeBy(600)
    assertThat(job.isCompleted).isTrue()
    assertThat(player.pauseCount).isEqualTo(1)
    assertThat(player.volume).isEqualTo(0.8f)
    assertThat(player.volumeWhenPaused).isEqualTo(0f)
  }

  @Test
  fun fade_volumeOnlyEverDecreasesUntilPause() = runTest {
    val player = FakeVolumePlayer(volume = 1f)

    VolumeFadeController.fade(
      scope = this,
      duration = 2.seconds,
      tickRate = 20,
      getVolume = { player.volume },
      setVolume = { player.volume = it },
      onPause = { player.pauseCount++ },
      now = { testScheduler.currentTime },
    )
    advanceTimeBy(2_100)

    val samples = player.history.takeWhile { it > 0f || player.history.indexOf(it) == 0 }
    assertThat(samples.size).isGreaterThan(10)
    samples.zipWithNext().forEach { (previous, next) ->
      assertThat(next).isLessThan(previous + 1e-6f)
    }
    // A linear ramp would still be at 0.5 half way through; the log curve is well below that
    val midpoint = player.history[player.history.size / 2]
    assertThat(midpoint).isLessThan(0.2f)
  }

  @Test
  fun fade_zeroDurationPausesImmediately() = runTest {
    val player = FakeVolumePlayer(volume = 0.6f)

    val job = VolumeFadeController.fade(
      scope = this,
      duration = Duration.ZERO,
      tickRate = 10,
      getVolume = { player.volume },
      setVolume = { player.volume = it },
      onPause = { player.pauseCount++ },
      now = { testScheduler.currentTime },
    )
    advanceTimeBy(1)

    assertThat(job.isCompleted).isTrue()
    assertThat(player.pauseCount).isEqualTo(1)
    assertThat(player.volume).isEqualTo(0.6f)
  }

  @Test
  fun fade_neverOutlivesTheDurationWhenTicksAreDelayed() = runTest {
    val player = FakeVolumePlayer(volume = 1f)

    val job = VolumeFadeController.fade(
      scope = this,
      duration = 1.seconds,
      tickRate = 2, // 500ms ticks: coarse enough that a naive step count would overshoot
      getVolume = { player.volume },
      setVolume = { player.volume = it },
      onPause = { player.pauseCount++ },
      now = { testScheduler.currentTime },
    )
    advanceTimeBy(1_001)

    assertThat(job.isCompleted).isTrue()
    assertThat(player.pauseCount).isEqualTo(1)
  }

  private class FakeVolumePlayer(volume: Float) {
    val history = mutableListOf<Float>()
    var pauseCount = 0
    var volumeWhenPaused: Float? = null
    var volume: Float = volume
      set(value) {
        field = value
        history += value
      }

    init {
      history += volume
    }
  }
}
