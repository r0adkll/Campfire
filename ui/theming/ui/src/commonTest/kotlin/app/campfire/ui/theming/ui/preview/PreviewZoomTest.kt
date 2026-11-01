// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class PreviewZoomTest {

  private val preview = Size(400f, 300f)

  @Test
  fun aSmallPartZoomsAsFarAsItGoesAndCenters() {
    val zoom = zoomTo(Rect(left = 180f, top = 140f, right = 220f, bottom = 144f), preview, padding = 16f)

    assertEquals(2.5f, zoom.scale)
    // The part's center, (200, 142), lands in the middle of the preview.
    assertEquals(Offset(200f - 200f * 2.5f, 150f - 142f * 2.5f), zoom.offset)
  }

  @Test
  fun aPartAsWideAsThePreviewDoesNotZoom() {
    // Like the navigation bar: zooming in would crop it, hiding the selected tab.
    val zoom = zoomTo(Rect(left = 0f, top = 260f, right = 400f, bottom = 300f), preview, padding = 16f)

    assertEquals(ZoomTarget.None, zoom)
  }

  @Test
  fun aPartZoomsOnlyAsFarAsKeepsItWhole() {
    // 200 wide with 16 of padding either side fits the 400-wide preview at 400 / 232.
    val zoom = zoomTo(Rect(left = 100f, top = 100f, right = 300f, bottom = 120f), preview, padding = 16f)

    assertEquals(400f / 232f, zoom.scale)
  }

  @Test
  fun aPartAtTheEdgeStaysAgainstItRatherThanShowingPastTheContent() {
    val zoom = zoomTo(Rect(left = 0f, top = 0f, right = 40f, bottom = 20f), preview, padding = 16f)

    assertEquals(Offset.Zero, zoom.offset)
  }

  @Test
  fun aPartBiggerThanThePreviewDoesNotZoomOut() {
    val zoom = zoomTo(Rect(left = 0f, top = 0f, right = 400f, bottom = 300f), preview, padding = 16f)

    assertEquals(ZoomTarget.None, zoom)
  }

  @Test
  fun anEmptyPreviewDoesNotZoom() {
    assertEquals(ZoomTarget.None, zoomTo(Rect(0f, 0f, 10f, 10f), Size.Zero, padding = 16f))
  }
}
