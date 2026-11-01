// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuilderLayoutTest {

  @Test
  fun phonesGetTheTray() {
    assertEquals(BuilderLayout.Tray, builderLayout(DpSize(412.dp, 830.dp), hinge = null))
    // Split screen on a tablet is phone-width too.
    assertEquals(BuilderLayout.Tray, builderLayout(DpSize(599.dp, 1000.dp), hinge = null))
  }

  @Test
  fun anUnfoldedFoldablePutsTheControlsBesideOnePreview() {
    assertEquals(
      BuilderLayout.Side(controlsWidth = 320.dp, gap = 8.dp, dual = false),
      builderLayout(DpSize(673.dp, 777.dp), hinge = null),
    )
  }

  @Test
  fun lightAndDarkPreviewsShowOnlyWhenBothFitAtAPhonesWidth() {
    // Wide enough for the old two previews, but they came out at about half size.
    assertEquals(
      BuilderLayout.Side(controlsWidth = 400.dp, gap = 8.dp, dual = false),
      builderLayout(DpSize(1000.dp, 609.dp), hinge = null),
    )
    assertEquals(
      BuilderLayout.Side(controlsWidth = 400.dp, gap = 8.dp, dual = true),
      builderLayout(DpSize(1280.dp, 736.dp), hinge = null),
    )
  }

  @Test
  fun aTabletopFoldPutsThePreviewAboveTheHingeAndTheTrayBelow() {
    val hinge = FoldHinge(start = 362.dp, end = 382.dp, isVertical = false)

    assertEquals(
      BuilderLayout.Tabletop(topHeight = 362.dp, hingeHeight = 20.dp),
      builderLayout(DpSize(900.dp, 788.dp), hinge),
    )
  }

  @Test
  fun aTabletopFoldThatLeavesTooLittleOnOneSideIsIgnored() {
    val hinge = FoldHinge(start = 600.dp, end = 620.dp, isVertical = false)

    assertEquals(BuilderLayout.Tray, builderLayout(DpSize(412.dp, 700.dp), hinge))
  }

  @Test
  fun aBookFoldLinesTheControlsUpWithTheHinge() {
    val hinge = FoldHinge(start = 420.dp, end = 440.dp, isVertical = true)

    assertEquals(
      BuilderLayout.Side(controlsWidth = 420.dp, gap = 20.dp, dual = false),
      builderLayout(DpSize(880.dp, 780.dp), hinge),
    )
  }

  @Test
  fun aBookFoldTooCloseToAnEdgeIsIgnored() {
    val hinge = FoldHinge(start = 200.dp, end = 200.dp, isVertical = true)

    assertEquals(
      BuilderLayout.Side(controlsWidth = 400.dp, gap = 8.dp, dual = false),
      builderLayout(DpSize(1000.dp, 780.dp), hinge),
    )
  }

  private val density = Density(2f)

  /** The builder's content under a 64dp top bar and 24dp status bar, on a 900dp-wide fold. */
  private val content = Rect(left = 0f, top = 176f, right = 1800f, bottom = 1800f)

  private fun hinge(bounds: Rect, vertical: Boolean, separating: Boolean = true) =
    HingeInfo(bounds, false, vertical, separating, false)

  @Test
  fun aTabletopHingeIsPlacedRelativeToTheContent() {
    val posture = Posture(true, listOf(hinge(Rect(0f, 900f, 1800f, 940f), vertical = false)))

    assertEquals(
      FoldHinge(start = 362.dp, end = 382.dp, isVertical = false),
      foldHinge(posture, content, density, playerDocked = false),
    )
  }

  @Test
  fun aTabletopHingeDoesNotCountWhenTheAppAlreadyDockedThePlayerBelowIt() {
    val posture = Posture(true, listOf(hinge(Rect(0f, 900f, 1800f, 940f), vertical = false)))

    assertNull(foldHinge(posture, content, density, playerDocked = true))
  }

  @Test
  fun aFlatHorizontalHingeDoesNotCount() {
    val posture = Posture(false, listOf(hinge(Rect(0f, 900f, 1800f, 940f), vertical = false)))

    assertNull(foldHinge(posture, content, density, playerDocked = false))
  }

  @Test
  fun aSeparatingVerticalHingeCounts() {
    val posture = Posture(false, listOf(hinge(Rect(880f, 0f, 920f, 1800f), vertical = true)))

    assertEquals(
      FoldHinge(start = 440.dp, end = 460.dp, isVertical = true),
      foldHinge(posture, content, density, playerDocked = false),
    )
  }

  @Test
  fun aFlatVerticalFoldDoesNotCount() {
    val posture = Posture(false, listOf(hinge(Rect(880f, 0f, 920f, 1800f), vertical = true, separating = false)))

    assertNull(foldHinge(posture, content, density, playerDocked = false))
  }
}
