// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test

/**
 * The marquee only takes over when the text cannot fit. While it scrolls, the text is laid out at
 * its full length inside the clipped viewport, so the text node growing wider than its container is
 * the observable sign that it switched over.
 */
@OptIn(ExperimentalTestApi::class)
class MarqueeTextTest {

  @Test
  fun `text that fits is laid out within its container`() = runComposeUiTest {
    setContent {
      MarqueeText(text = ShortText, modifier = Modifier.width(ContainerWidth))
    }

    assertThat(onNodeWithText(ShortText).getUnclippedBoundsInRoot().width).isLessThanOrEqualTo(ContainerWidth)
  }

  @Test
  fun `text that overflows scrolls at its full length`() = runComposeUiTest {
    setContent {
      MarqueeText(text = LongText, modifier = Modifier.width(ContainerWidth))
    }

    assertThat(onNodeWithText(LongText).getUnclippedBoundsInRoot().width).isGreaterThan(ContainerWidth)
  }

  @Test
  fun `auto sized text only scrolls once it overflows at its smallest size`() = runComposeUiTest {
    setContent {
      MarqueeText(
        text = MediumText,
        autoSize = TextAutoSize.StepBased(minFontSize = 4.sp, maxFontSize = 28.sp),
        modifier = Modifier.width(ContainerWidth),
      )
    }

    assertThat(onNodeWithText(MediumText).getUnclippedBoundsInRoot().width).isLessThanOrEqualTo(ContainerWidth)
  }

  @Test
  fun `text that later fits stops scrolling`() = runComposeUiTest {
    var text by mutableStateOf(LongText)
    setContent {
      MarqueeText(text = text, modifier = Modifier.width(ContainerWidth))
    }
    assertThat(onNodeWithText(LongText).getUnclippedBoundsInRoot().width).isGreaterThan(ContainerWidth)

    text = ShortText
    waitForIdle()

    assertThat(onNodeWithText(ShortText).getUnclippedBoundsInRoot().width).isLessThanOrEqualTo(ContainerWidth)
  }

  @Test
  fun `overflowing text is ellipsized when scrolling is off`() = runComposeUiTest {
    setContent {
      MarqueeText(text = LongText, scrollEnabled = false, modifier = Modifier.width(ContainerWidth))
    }

    assertThat(onNodeWithText(LongText).getUnclippedBoundsInRoot().width).isLessThanOrEqualTo(ContainerWidth)
  }

  @Test
  fun `the marquee rests before each pass and stops after its last`() {
    val pass = 1_000L
    val cycle = MarqueeDelayMillis + pass

    assertThat(isMarqueeMoving(0, pass)).isFalse()
    assertThat(isMarqueeMoving(MarqueeDelayMillis - 1L, pass)).isFalse()
    assertThat(isMarqueeMoving(MarqueeDelayMillis.toLong(), pass)).isTrue()
    assertThat(isMarqueeMoving(cycle - 1, pass)).isTrue()
    assertThat(isMarqueeMoving(cycle, pass)).isFalse()
    assertThat(isMarqueeMoving(cycle * (MarqueeIterations - 1) + MarqueeDelayMillis, pass)).isTrue()
    assertThat(isMarqueeMoving(cycle * MarqueeIterations, pass)).isFalse()
    assertThat(isMarqueeMoving(cycle * MarqueeIterations * 10, pass)).isFalse()
  }

  @Test
  fun `a pass lasts as long as scrolling its distance at the marquee's velocity`() {
    assertThat(marqueePassMillis(distancePx = 300, velocityPxPerSecond = 30f)).isEqualTo(10_000L)
    assertThat(marqueePassMillis(distancePx = 301, velocityPxPerSecond = 30f)).isEqualTo(10_034L)
  }

  private companion object {
    val ContainerWidth = 120.dp
    const val ShortText = "Ch. 1"
    const val MediumText = "Chapter One: The Beginning"
    const val LongText = "Chapter One Hundred and Twelve: In Which Everything Goes Terribly, Terribly Wrong"
  }
}
