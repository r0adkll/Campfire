// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.ui.cast

import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import kotlin.test.Test

/**
 * The picker's card used to sit in the top-end corner of the popup's full-screen box — fine on a
 * phone, where the cast button is already in that corner, wrong everywhere the player is inset.
 * These assert the card's corner lands on the button's instead.
 */
class CastPopupPlacementTest {

  private val margin = 12
  private val preferredWidth = 320
  private val box = IntSize(1600, 1000)

  private fun placement(anchor: IntRect, container: IntSize = box) = castPopupPlacement(
    anchorBounds = anchor,
    containerSize = container,
    edgeMargin = margin,
    preferredWidth = preferredWidth,
  )

  @Test
  fun `an inset button gets the card's corner on its own, not the box's`() {
    // The supporting-pane player: its top bar's trailing action sits well inside the screen.
    val button = IntRect(left = 900, top = 120, right = 948, bottom = 168)
    val result = placement(button)

    assertThat(result.alignment).isEqualTo(AbsoluteAlignment.TopRight)
    // Card's right edge on the button's right edge, card's top on the button's top.
    assertThat(box.width - result.right).isEqualTo(button.right)
    assertThat(result.top).isEqualTo(button.top)
    assertThat(result.width).isEqualTo(preferredWidth)
  }

  @Test
  fun `the button ends up underneath the card`() {
    val button = IntRect(left = 900, top = 120, right = 948, bottom = 168)
    val result = placement(button)

    val cardRight = box.width - result.right
    assertThat(cardRight - result.width).isLessThanOrEqualTo(button.left)
    assertThat(cardRight).isGreaterThanOrEqualTo(button.right)
    assertThat(result.top).isLessThanOrEqualTo(button.top)
  }

  @Test
  fun `a button on the leading edge opens towards the screen instead of off it`() {
    // The wide-and-short player's rail: trailing actions are pinned bottom-leading.
    val button = IntRect(left = 24, top = 900, right = 72, bottom = 948)
    val result = placement(button)

    assertThat(result.alignment).isEqualTo(AbsoluteAlignment.BottomLeft)
    assertThat(result.left).isEqualTo(button.left)
    assertThat(box.height - result.bottom).isEqualTo(button.bottom)
  }

  @Test
  fun `a button at the screen's edge still leaves the card a margin`() {
    val phone = IntSize(1080, 2400)
    val button = IntRect(left = 1040, top = 4, right = 1080, bottom = 44)
    val result = placement(button, phone)

    assertThat(result.right).isEqualTo(margin)
    assertThat(result.top).isEqualTo(margin)
  }

  @Test
  fun `a narrow screen trims the card rather than letting it overhang`() {
    val narrow = IntSize(300, 600)
    val result = placement(IntRect(left = 260, top = 40, right = 300, bottom = 80), narrow)

    assertThat(result.width).isEqualTo(300 - margin - margin)
    assertThat(result.left + result.width + result.right).isLessThanOrEqualTo(narrow.width)
  }

  @Test
  fun `the card is only as tall as the room it opens into`() {
    val result = placement(IntRect(left = 900, top = 120, right = 948, bottom = 168))

    assertThat(result.top + result.maxHeight + result.bottom).isEqualTo(box.height)
  }

  @Test
  fun `an unmeasured box does not blow up`() {
    val result = placement(IntRect.Zero, IntSize.Zero)

    assertThat(result.width).isEqualTo(0)
    assertThat(result.maxHeight).isEqualTo(0)
  }
}
