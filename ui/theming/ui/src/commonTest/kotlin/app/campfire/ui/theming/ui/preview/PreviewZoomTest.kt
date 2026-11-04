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

  /** A phone's screen, as a scene is laid out while zoomed, behind a short preview of [short]. */
  private val phone = Size(360f, 640f)
  private val short = Size(328f, 180f)

  @Test
  fun aSmallPartZoomsAsFarAsItGoesAndCenters() {
    val zoom = zoomTo(Rect(left = 180f, top = 140f, right = 220f, bottom = 144f), preview, preview, padding = 16f)

    assertEquals(MaxZoom, zoom.scale)
    // The part's center, (200, 142), lands in the middle of the preview.
    assertEquals(Offset(200f - 200f * MaxZoom, 150f - 142f * MaxZoom), zoom.offset)
  }

  @Test
  fun aPartAsWideAsThePreviewDoesNotZoom() {
    // Like the navigation bar: zooming in would crop it, hiding the selected tab.
    val zoom = zoomTo(Rect(left = 0f, top = 260f, right = 400f, bottom = 300f), preview, preview, padding = 16f)

    assertEquals(ZoomTarget.None, zoom)
  }

  @Test
  fun aPartZoomsOnlyAsFarAsKeepsItWhole() {
    // 300 wide with 16 of padding either side fits the 400-wide preview at 400 / 332.
    val zoom = zoomTo(Rect(left = 50f, top = 100f, right = 350f, bottom = 120f), preview, preview, padding = 16f)

    assertEquals(400f / 332f, zoom.scale)
  }

  @Test
  fun aPartAtTheEdgeStaysAgainstItRatherThanShowingPastTheContent() {
    val zoom = zoomTo(Rect(left = 0f, top = 0f, right = 40f, bottom = 20f), preview, preview, padding = 16f)

    assertEquals(Offset.Zero, zoom.offset)
  }

  @Test
  fun aPartBiggerThanThePreviewDoesNotZoomOut() {
    val zoom = zoomTo(Rect(left = 0f, top = 0f, right = 400f, bottom = 300f), preview, preview, padding = 16f)

    assertEquals(ZoomTarget.None, zoom)
  }

  @Test
  fun anEmptyPreviewDoesNotZoom() {
    assertEquals(ZoomTarget.None, zoomTo(Rect(0f, 0f, 10f, 10f), Size.Zero, preview, padding = 16f))
  }

  @Test
  fun aPartOfAPhoneSizedSceneShowsWholeInAShortPreview() {
    // A card's title and author: 136 x 68, two thirds of the way down a phone's screen.
    val part = Rect(left = 16f, top = 400f, right = 152f, bottom = 468f)
    val zoom = zoomTo(part, short, phone, padding = 16f)

    // 68 tall with padding is 100, so the 180-tall preview would fit it at 1.8, past the most it zooms.
    assertEquals(MaxZoom, zoom.scale)
    val shown = Rect(
      left = part.left * zoom.scale + zoom.offset.x,
      top = part.top * zoom.scale + zoom.offset.y,
      right = part.right * zoom.scale + zoom.offset.x,
      bottom = part.bottom * zoom.scale + zoom.offset.y,
    )
    assertEquals(90f, shown.center.y, absoluteTolerance = 0.01f)
    // Against the scene's left edge, which stays against the preview's.
    assertEquals(0f, zoom.offset.x)
  }

  @Test
  fun aPartTooWideToReadWholeIsCutRatherThanShrunk() {
    // A settings row as wide as a 480-wide scene, in a 328-wide preview: whole, it would show at 0.64.
    val canvas = Size(480f, 500f)
    val zoom = zoomTo(Rect(left = 0f, top = 200f, right = 480f, bottom = 256f), short, canvas, padding = 16f)

    assertEquals(MinZoom, zoom.scale)
    // Centered on the row: its middle, 240 across, lands in the preview's middle.
    assertEquals(164f - 240f * MinZoom, zoom.offset.x)
  }

  @Test
  fun aSceneShorterThanThePreviewAtTheZoomIsCentered() {
    // A tall, narrow preview zoomed on a part near the top of a wide, short scene.
    val zoom =
      zoomTo(Rect(left = 0f, top = 0f, right = 40f, bottom = 20f), Size(100f, 1000f), Size(400f, 100f), padding = 0f)

    assertEquals(MaxZoom, zoom.scale)
    assertEquals((1000f - 100f * MaxZoom) / 2, zoom.offset.y)
  }

  @Test
  fun aShortPreviewFitsTheWholePhoneSizedSceneCentered() {
    val fit = fitTarget(short, phone)

    assertEquals(180f / 640f, fit.scale)
    assertEquals(Offset((328f - 360f * fit.scale) / 2, 0f), fit.offset)
  }

  @Test
  fun aWholeSceneTakesThePreviewsShapeScaledToFit() {
    val canvas = canvasSize(short, minSize = Size(360f, 380f), focusSize = phone, focused = false)

    // Scaled to 180 / 380 to fit the preview's height, at the preview's shape.
    assertEquals(328f * 380f / 180f, canvas.width, absoluteTolerance = 0.01f)
    assertEquals(380f, canvas.height, absoluteTolerance = 0.01f)
    val fit = fitTarget(short, canvas)
    assertEquals(180f / 380f, fit.scale, absoluteTolerance = 0.0001f)
    assertEquals(0f, fit.offset.x, absoluteTolerance = 0.01f)
    assertEquals(0f, fit.offset.y, absoluteTolerance = 0.01f)
  }

  @Test
  fun aZoomedSceneHasAPhonesRoomWhateverThePreviewsShape() {
    assertEquals(phone, canvasSize(short, minSize = Size(360f, 380f), focusSize = phone, focused = true))
    // A preview bigger than a phone gives the scene its own size, as before.
    val big = Size(600f, 800f)
    assertEquals(big, canvasSize(big, minSize = Size(360f, 380f), focusSize = phone, focused = true))
  }
}
