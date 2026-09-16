// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.root.ui

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.isLandscapePhone
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

/**
 * A Pixel Fold-shaped inner display: 1800×1800px at 2x, hinge across the middle. The upper
 * region works out to 900×450dp, which is exactly the landscape-phone size class.
 */
class TabletopSplitTest {

  private val density = Density(2f)
  private val window = IntSize(1800, 1800)

  private fun hinge(top: Float, bottom: Float, vertical: Boolean = false) = HingeInfo(
    Rect(0f, top, 1800f, bottom),
    false,
    vertical,
    true,
    false,
  )

  @Test
  fun `no split outside tabletop posture`() {
    val flat = Posture(false, listOf(hinge(900f, 940f)))
    assertThat(computeTabletopSplit(flat, window, density)).isNull()
  }

  @Test
  fun `the horizontal hinge divides the window and the top lays out as a landscape phone`() {
    val tabletop = Posture(true, listOf(hinge(900f, 940f)))

    val split = computeTabletopSplit(tabletop, window, density)

    assertThat(split).isNotNull()
    assertThat(split!!.topHeight).isEqualTo(450.dp)
    assertThat(split.hingeHeight).isEqualTo(20.dp)
    assertThat(split.topSizeClass.isLandscapePhone).isTrue()
  }

  @Test
  fun `a vertical hinge is not a tabletop fold, so the window halves with no gap`() {
    val posture = Posture(true, listOf(hinge(0f, 1800f, vertical = true)))

    val split = computeTabletopSplit(posture, window, density)!!

    assertThat(split.topHeight).isEqualTo(450.dp)
    assertThat(split.hingeHeight).isEqualTo(0.dp)
  }

  @Test
  fun `a hinge that would leave the player a sliver yields no split`() {
    // Nonsense bounds the platform should never report — docking a player into what is left
    // would give it nothing to draw in
    val posture = Posture(true, listOf(hinge(1700f, 2000f)))

    assertThat(computeTabletopSplit(posture, window, density)).isNull()
  }

  @Test
  fun `an off-centre but usable hinge still splits`() {
    val posture = Posture(true, listOf(hinge(1200f, 1240f)))

    val split = computeTabletopSplit(posture, window, density)!!

    assertThat(split.topHeight).isEqualTo(600.dp)
    assertThat(split.hingeHeight).isEqualTo(20.dp)
    assertThat(split.topSizeClass.isLandscapePhone).isFalse()
  }

  @Test
  fun `an unmeasured window yields no split`() {
    val tabletop = Posture(true, listOf(hinge(900f, 940f)))
    assertThat(computeTabletopSplit(tabletop, IntSize(0, 0), density)).isNull()
  }
}
