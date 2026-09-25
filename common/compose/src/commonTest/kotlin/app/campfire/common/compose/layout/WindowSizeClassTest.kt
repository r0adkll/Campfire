// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.layout

import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

/**
 * [isLandscapePhone] decides whether the player lays out sideways, and it used to be gated on the
 * Expanded width breakpoint (840dp) — which a large share of phones do not reach in landscape,
 * leaving them with the portrait arrangement in 360dp of height. These sizes are the ones that
 * regression turned on, measured as the device reports them.
 */
class WindowSizeClassTest {

  @Test
  fun `phones in landscape are wide and short, including those under 840dp`() {
    // Well over the old Expanded bar.
    assertThat(sizeClass(width = 914, height = 411).isLandscapePhone).isTrue() // Pixel 8
    assertThat(sizeClass(width = 852, height = 393).isLandscapePhone).isTrue() // iPhone 15

    // Under it — these are the devices that regressed.
    assertThat(sizeClass(width = 832, height = 384).isLandscapePhone).isTrue() // S24+, default res
    assertThat(sizeClass(width = 812, height = 375).isLandscapePhone).isTrue() // iPhone 13 mini
    assertThat(sizeClass(width = 780, height = 360).isLandscapePhone).isTrue() // Galaxy S24
  }

  @Test
  fun `a half-open foldable's upper region is wide and short too`() {
    assertThat(sizeClass(width = 840, height = 340).isLandscapePhone).isTrue()
  }

  @Test
  fun `phones in portrait are not`() {
    assertThat(sizeClass(width = 411, height = 914).isLandscapePhone).isFalse()
    assertThat(sizeClass(width = 360, height = 780).isLandscapePhone).isFalse()
  }

  @Test
  fun `a tablet is not, in either orientation`() {
    assertThat(sizeClass(width = 800, height = 1280).isLandscapePhone).isFalse()
    assertThat(sizeClass(width = 1280, height = 800).isLandscapePhone).isFalse()
  }

  @Test
  fun `a window too narrow to lay out sideways is not, however short`() {
    assertThat(sizeClass(width = 480, height = 360).isLandscapePhone).isFalse()
  }

  private fun sizeClass(width: Int, height: Int): WindowSizeClass =
    WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width.toFloat(), height.toFloat())
}
