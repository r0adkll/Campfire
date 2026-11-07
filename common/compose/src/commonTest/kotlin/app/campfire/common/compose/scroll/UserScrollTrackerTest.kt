// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.scroll

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class UserScrollTrackerTest {

  private fun TestScope.tracker() = UserScrollTracker(
    scope = backgroundScope,
    settleTime = 200.milliseconds,
    timeSource = testScheduler.timeSource,
  )

  private fun UserScrollTracker.scroll(delta: Offset = Offset(0f, 12f)) =
    nestedScrollConnection.onPreScroll(delta, NestedScrollSource.UserInput)

  @Test
  fun `starts scrolling on the first delta and consumes none of it`() = runTest {
    val tracker = tracker()

    val consumed = tracker.scroll()

    assertThat(consumed).isEqualTo(Offset.Zero)
    assertThat(tracker.isScrolling).isTrue()
  }

  @Test
  fun `stays scrolling while deltas keep arriving, and settles once they stop`() = runTest {
    val tracker = tracker()

    tracker.scroll()
    repeat(5) {
      advanceTimeBy(150.milliseconds)
      tracker.scroll()
      runCurrent()
      assertThat(tracker.isScrolling).isTrue()
    }

    advanceTimeBy(150.milliseconds)
    runCurrent()
    assertThat(tracker.isScrolling).isTrue()

    advanceTimeBy(60.milliseconds)
    runCurrent()
    assertThat(tracker.isScrolling).isFalse()
  }

  @Test
  fun `a zero delta is not scrolling`() = runTest {
    val tracker = tracker()

    tracker.scroll(Offset.Zero)

    assertThat(tracker.isScrolling).isFalse()
  }

  @Test
  fun `a later gesture scrolls again`() = runTest {
    val tracker = tracker()
    tracker.scroll()
    advanceTimeBy(300.milliseconds)
    runCurrent()
    assertThat(tracker.isScrolling).isFalse()

    tracker.scroll(Offset(24f, 0f))

    assertThat(tracker.isScrolling).isTrue()
  }

  @Test
  fun `awaitNextScroll returns once a scroll has started and settled`() = runTest {
    val tracker = tracker()
    val waiting = async { tracker.awaitNextScroll() }
    advanceTimeBy(1_000.milliseconds)
    runCurrent()
    assertThat(waiting.isCompleted).isFalse()

    tracker.scroll()
    advanceTimeBy(100.milliseconds)
    runCurrent()
    assertThat(waiting.isCompleted).isFalse()

    advanceTimeBy(150.milliseconds)
    runCurrent()
    assertThat(waiting.isCompleted).isTrue()
  }
}
