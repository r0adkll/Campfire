// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.scroll.LocalUserScrollTracker
import app.campfire.common.compose.scroll.UserScrollTracker
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

@OptIn(ExperimentalTestApi::class)
class CardMarqueeTest {

  @Test
  fun `waits out the dwell, then scrolls`() = runComposeUiTest {
    setMarquee(text = LongTitle)

    val atRest = snapshot()
    advance(1_500)
    assertThat(snapshot() sameAs atRest).isTrue()

    advance(1_000)
    assertThat(snapshot() sameAs atRest).isFalse()
  }

  @Test
  fun `pauses in place while the user scrolls, and carries on once it settles`() = runComposeUiTest {
    val tracker = setMarquee(text = LongTitle)
    val atRest = snapshot()
    advance(2_500)

    // A scroll that keeps going for a while
    tracker.scroll()
    mainClock.advanceTimeByFrame()
    val paused = snapshot()
    repeat(4) {
      advance(150)
      tracker.scroll()
    }

    // Held where it was, not snapped back to the start
    assertThat(snapshot() sameAs paused).isTrue()
    assertThat(paused sameAs atRest).isFalse()

    // Settled: it moves on from there
    advance(600)
    assertThat(snapshot() sameAs paused).isFalse()
  }

  @Test
  fun `a scroll during the dwell starts the wait over`() = runComposeUiTest {
    val tracker = setMarquee(text = LongTitle)
    val atRest = snapshot()

    advance(1_800)
    tracker.scroll()
    // 2.4s since it appeared, but only 0.4s since the list settled
    advance(600)
    assertThat(snapshot() sameAs atRest).isTrue()

    advance(2_000)
    assertThat(snapshot() sameAs atRest).isFalse()
  }

  @Test
  fun `text that fits never moves`() = runComposeUiTest {
    setMarquee(text = "Short")

    val atRest = snapshot()
    advance(10_000)
    assertThat(snapshot() sameAs atRest).isTrue()
  }

  // The tracker times its settling and the marquee's dwell on its own scope; this runs it on
  // virtual time, which advance() moves along with the frame clock
  private val timers = TestCoroutineScheduler()
  private val tracker = UserScrollTracker(
    scope = CoroutineScope(StandardTestDispatcher(timers)),
    timeSource = timers.timeSource,
  )

  private fun ComposeUiTest.advance(millis: Long) {
    var left = millis
    while (left > 0) {
      val step = minOf(left, 16L)
      timers.advanceTimeBy(step)
      timers.runCurrent()
      mainClock.advanceTimeBy(step)
      left -= step
    }
  }

  private fun ComposeUiTest.setMarquee(text: String): UserScrollTracker {
    mainClock.autoAdvance = false
    setContent {
      CompositionLocalProvider(
        LocalDensity provides Density(1f),
        LocalUserScrollTracker provides tracker,
      ) {
        Box(Modifier.width(100.dp).testTag(Tag)) {
          BasicText(
            text = text,
            maxLines = 1,
            modifier = Modifier.cardMarquee(enabled = true, dwellMillis = 2_000),
          )
        }
      }
    }
    mainClock.advanceTimeByFrame()
    return tracker
  }

  private fun ComposeUiTest.snapshot(): IntArray =
    onNodeWithTag(Tag).captureToImage().toPixelMap().buffer.copyOf()

  private fun UserScrollTracker.scroll() {
    nestedScrollConnection.onPreScroll(Offset(0f, 12f), NestedScrollSource.UserInput)
  }

  private infix fun IntArray.sameAs(other: IntArray): Boolean = contentEquals(other)

  private companion object {
    const val Tag = "marquee"
    const val LongTitle = "A title far too long to fit on the card it is shown on"
  }
}
