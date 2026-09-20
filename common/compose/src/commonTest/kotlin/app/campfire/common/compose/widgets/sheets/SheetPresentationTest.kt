// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.sheets

import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/**
 * The sizes each presentation has to cover, measured as the device or region reports them. A
 * bottom sheet on a short region leaves roughly two list rows, which is what the side panel is
 * here to fix; everything tall enough keeps the sheet the content was written for.
 */
class SheetPresentationTest {

  @Test
  fun `tall regions keep the bottom sheet`() {
    assertThat(presentationFor(411, 914)).isEqualTo(SheetPresentation.Bottom) // phone, portrait
    assertThat(presentationFor(800, 1280)).isEqualTo(SheetPresentation.Bottom) // tablet
    assertThat(presentationFor(400, 680)).isEqualTo(SheetPresentation.Bottom) // mini-player window
  }

  @Test
  fun `wide short regions get the side panel`() {
    assertThat(presentationFor(914, 411)).isEqualTo(SheetPresentation.Side) // Pixel 8, sideways
    assertThat(presentationFor(780, 360)).isEqualTo(SheetPresentation.Side) // Galaxy S24, sideways
    assertThat(presentationFor(840, 340)).isEqualTo(SheetPresentation.Side) // foldable, lower half
  }

  @Test
  fun `regions too short and too narrow for either get the card`() {
    assertThat(presentationFor(480, 360)).isEqualTo(SheetPresentation.Dialog)
    assertThat(presentationFor(380, 380)).isEqualTo(SheetPresentation.Dialog)
  }

  @Test
  fun `the boundary sits on the Medium breakpoints`() {
    // Height: 480dp is the Medium bound, so it is the first that is not compact.
    assertThat(presentationFor(900, 480)).isEqualTo(SheetPresentation.Bottom)
    assertThat(presentationFor(900, 479)).isEqualTo(SheetPresentation.Side)

    // Width: 600dp is the Medium bound, and below it there is no room for a panel.
    assertThat(presentationFor(600, 400)).isEqualTo(SheetPresentation.Side)
    assertThat(presentationFor(599, 400)).isEqualTo(SheetPresentation.Dialog)
  }

  private fun presentationFor(width: Int, height: Int): SheetPresentation =
    WindowSizeClass.BREAKPOINTS_V2
      .computeWindowSizeClass(width.toFloat(), height.toFloat())
      .sheetPresentation()
}
