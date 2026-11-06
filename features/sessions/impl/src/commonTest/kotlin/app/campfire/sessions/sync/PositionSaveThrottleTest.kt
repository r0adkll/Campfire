// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.sync

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest

class PositionSaveThrottleTest {

  private var now = 0L
  private val throttle = PositionSaveThrottle { now }

  /** Advances the clock by one player tick and reports the position [time] seconds in. */
  private suspend fun tick(time: Double, item: String = "book"): Boolean {
    now += 500
    return throttle.onPosition(item, (time * 1000).toLong().milliseconds)
  }

  @Test
  fun `while playing, saves the first position then at most every save interval`() = runTest {
    throttle.onPlayingChanged(true)

    assertThat(tick(0.5)).isTrue()
    // 4.5s of ticks after that save
    val saves = (2..10).map { tick(it * 0.5) }
    assertThat(saves.none { it }).isTrue()
    // the tick that reaches 5s since the save
    assertThat(tick(5.5)).isTrue()
    assertThat(tick(6.0)).isFalse()
  }

  @Test
  fun `while playing, a jump is saved straight away`() = runTest {
    throttle.onPlayingChanged(true)
    tick(100.0)

    assertThat(tick(130.0)).isTrue()
    // a rewind is a jump too
    assertThat(tick(110.0)).isTrue()
    // but a small nudge waits for the interval
    assertThat(tick(105.0)).isFalse()
  }

  @Test
  fun `outside playback every position is saved`() = runTest {
    assertThat(tick(10.0)).isTrue()
    assertThat(tick(10.5)).isTrue()
    assertThat(tick(11.0)).isTrue()
  }

  @Test
  fun `a different item saves straight away`() = runTest {
    throttle.onPlayingChanged(true)
    tick(10.0, item = "first")

    assertThat(tick(1.0, item = "second")).isTrue()
  }

  @Test
  fun `flush hands back the unsaved position once`() = runTest {
    throttle.onPlayingChanged(true)
    tick(10.0)
    tick(10.5)
    tick(11.0)

    assertThat(throttle.flush("book")).isEqualTo(11.seconds)
    assertThat(throttle.flush("book")).isNull()
  }

  @Test
  fun `flush has nothing when the latest position was saved`() = runTest {
    throttle.onPlayingChanged(true)
    tick(10.0)

    assertThat(throttle.flush("book")).isNull()
  }

  @Test
  fun `flush only answers for the item that was playing`() = runTest {
    throttle.onPlayingChanged(true)
    tick(10.0, item = "first")
    tick(10.5, item = "first")

    assertThat(throttle.flush("second")).isNull()
    assertThat(throttle.flush("first")).isEqualTo(10.5.seconds)
  }

  @Test
  fun `after a flush the interval restarts`() = runTest {
    throttle.onPlayingChanged(true)
    tick(10.0)
    tick(10.5)
    throttle.flush("book")

    assertThat(tick(11.0)).isFalse()
  }
}
