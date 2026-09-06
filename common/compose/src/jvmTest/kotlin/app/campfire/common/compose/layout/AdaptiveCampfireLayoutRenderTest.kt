// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.navigation.LocalUserSession
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.test.user
import app.campfire.core.session.UserSession
import com.slack.circuit.overlay.rememberOverlayHost
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * Renders [AdaptiveCampfireLayout] off-screen at a desktop Large width with the supporting pane
 * open, writing PNGs to `build/renders` for eyeballing. The assertions measure the pane from the
 * pixels: the pane content is painted a flat colour, so the first column of that colour along a
 * scanline is the pane's leading edge.
 */
@OptIn(ExperimentalComposeUiApi::class)
class AdaptiveCampfireLayoutRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `pane opens at the size-class default when nothing is stored`() {
    val width = renderAndMeasure("supporting-pane-default", storedWidth = null)
    assertEquals(SupportingContentWidthLarge, width)
  }

  @Test
  fun `stored width is clamped into the allowed range`() {
    assertEquals(SupportingContentMinWidth, renderAndMeasure("supporting-pane-clamped-min", storedWidth = 100.dp))
    assertEquals(SupportingContentMaxWidth, renderAndMeasure("supporting-pane-clamped-max", storedWidth = 2000.dp))
  }

  @Test
  fun `dragging the handle towards the start widens the pane and commits on release`() {
    var committed: Dp? = null
    scene(storedWidth = null, onWidthChange = { committed = it }).use { scene ->
      scene.settle()
      val edgeX = (WIDTH - SupportingContentWidthLarge.value) * DENSITY
      val y = HEIGHT * DENSITY / 2f

      scene.sendPointerEvent(PointerEventType.Move, Offset(edgeX, y))
      scene.sendPointerEvent(PointerEventType.Press, Offset(edgeX, y), button = PointerButton.Primary)
      var x = edgeX
      repeat(5) {
        x -= 30f * DENSITY
        scene.sendPointerEvent(PointerEventType.Move, Offset(x, y))
        scene.render()
      }
      val mid = scene.render(nanoTime = 2_000_000_000L)
      save("supporting-pane-dragging", mid)
      scene.sendPointerEvent(PointerEventType.Release, Offset(x, y), button = PointerButton.Primary)
      val after = scene.render(nanoTime = 3_000_000_000L)
      save("supporting-pane-dragged", after)

      // Drag slop eats a fraction of a pixel, so allow sub-dp drift
      assertWithinOneDp(SupportingContentWidthLarge + 150.dp, assertNotNull(committed))
      assertWithinOneDp(SupportingContentWidthLarge + 150.dp, after.measurePaneWidth())
    }
  }

  private fun renderAndMeasure(name: String, storedWidth: Dp?): Dp =
    scene(storedWidth, onWidthChange = {}).use { scene ->
      scene.settle()
      val image = scene.render(nanoTime = 2_000_000_000L)
      save(name, image)
      image.measurePaneWidth()
    }

  private fun scene(storedWidth: Dp?, onWidthChange: (Dp) -> Unit) = ImageComposeScene(
    width = (WIDTH * DENSITY).toInt(),
    height = (HEIGHT * DENSITY).toInt(),
    density = Density(DENSITY),
  ) {
    Layout(storedWidth, onWidthChange)
  }

  @Composable
  private fun Layout(storedWidth: Dp?, onWidthChange: (Dp) -> Unit) {
    CampfireTheme(useDarkColors = false) {
      CompositionLocalProvider(
        LocalWindowSizeClass provides WindowSizeClass(minWidthDp = WIDTH, minHeightDp = HEIGHT),
        LocalUserSession provides UserSession.LoggedIn(user("user")),
      ) {
        AdaptiveCampfireLayout(
          overlayHost = rememberOverlayHost(),
          drawerState = rememberDrawerState(DrawerValue.Closed),
          drawerEnabled = true,
          drawerContent = {},
          bottomBarNavigation = {},
          railNavigation = { Box(Modifier.width(80.dp).fillMaxHeight().background(Color.Gray)) },
          wideRailNavigation = { Box(Modifier.width(96.dp).fillMaxHeight().background(Color.DarkGray)) },
          content = {
            Box(Modifier.fillMaxSize().background(ContentColor)) {
              Text("Content", color = Color.White)
            }
          },
          playbackBarContent = { Box(Modifier.fillMaxWidth().height(72.dp).background(Color.Black)) },
          supportingContent = {
            Box(Modifier.fillMaxSize().background(PaneColor)) {
              Text("Detail", color = Color.White)
            }
          },
          showSupportingContent = true,
          supportingContentWidth = storedWidth,
          onSupportingContentWidthChange = onWidthChange,
        )
      }
    }
  }

  private fun assertWithinOneDp(expected: Dp, actual: Dp) {
    assertTrue(abs(expected.value - actual.value) < 1f, "expected $expected within 1dp, was $actual")
  }

  /** Runs the open animation to completion. */
  private fun ImageComposeScene.settle() {
    render()
    render(nanoTime = 1_000_000_000L)
  }

  private fun save(name: String, image: Image) {
    val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
    File(renders, "$name.png").writeBytes(bytes)
    println("rendered ${File(renders, "$name.png").absolutePath}")
    assertTrue(bytes.size > 1_000)
  }

  /**
   * Width of the pane in dp, measured on a scanline a quarter of the way down (clear of the
   * rounded top corner and the centred drag handle).
   */
  private fun Image.measurePaneWidth(): Dp {
    val bitmap = Bitmap().also {
      it.allocPixels(imageInfo)
      assertTrue(readPixels(it))
    }
    val y = height / 4
    val paneStart = (0 until width).first { x -> bitmap.getColor(x, y).isPane() }
    return ((width - paneStart) / DENSITY).dp
  }

  private fun Int.isPane(): Boolean {
    val r = (this shr 16) and 0xff
    val g = (this shr 8) and 0xff
    val b = this and 0xff
    return abs(r - 0xC0) < 8 && abs(g - 0x30) < 8 && abs(b - 0x60) < 8
  }

  private companion object {
    const val WIDTH = 1400
    const val HEIGHT = 900
    const val DENSITY = 2f
    val ContentColor = Color(0xFF3060C0)
    val PaneColor = Color(0xFFC03060)
  }
}
