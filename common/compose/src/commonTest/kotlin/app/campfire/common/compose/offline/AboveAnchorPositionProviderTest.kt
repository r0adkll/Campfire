// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.offline

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/**
 * The work-offline menu opens from the drawer's footer, near the window's bottom edge. Material's
 * own placement keeps 48dp clear of that edge and, short of it, left the menu floating well above
 * the button; these pin it to the anchor. Density 1, so the 8px shadow clearance is 8dp.
 */
class AboveAnchorPositionProviderTest {

  private val provider = AboveAnchorPositionProvider(Density(1f))
  private val window = IntSize(400, 800)
  private val menu = IntSize(240, 300)

  @Test
  fun `opens on the anchor's top edge, even within 48dp of the window's bottom`() {
    val anchor = IntRect(left = 16, top = 770, right = 72, bottom = 800)

    val position = provider.calculatePosition(anchor, window, LayoutDirection.Ltr, menu)

    // The visible menu, inside the clearance, lines up with the anchor's start and top
    assertThat(position).isEqualTo(IntOffset(x = 16 - 8, y = 770 + 8 - 300))
    assertThat(provider.transformOrigin.pivotFractionY).isEqualTo(1f)
  }

  @Test
  fun `aligns to the anchor's end in right-to-left layouts`() {
    val anchor = IntRect(left = 328, top = 700, right = 384, bottom = 748)

    val position = provider.calculatePosition(anchor, window, LayoutDirection.Rtl, menu)

    assertThat(position.x).isEqualTo(384 + 8 - 240)
  }

  @Test
  fun `opens below when there's more room there than above`() {
    val anchor = IntRect(left = 16, top = 100, right = 72, bottom = 148)

    val position = provider.calculatePosition(anchor, window, LayoutDirection.Ltr, menu)

    assertThat(position.y).isEqualTo(148 - 8)
    assertThat(provider.transformOrigin).isEqualTo(TransformOrigin((44f - 8f) / 240f, 0f))
  }

  @Test
  fun `stays inside the window`() {
    val anchor = IntRect(left = 0, top = 500, right = 56, bottom = 548)

    val position = provider.calculatePosition(anchor, window, LayoutDirection.Ltr, IntSize(240, 600))

    // Too little room above for the whole menu, but more than below: pinned to the window's top
    assertThat(position).isEqualTo(IntOffset(x = 0, y = 0))
  }
}
