// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.theme.CampfireTheme
import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isNotEqualTo
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * The side sheet drawn over stand-in content at the sizes that select it. PNGs land in
 * `build/renders` for eyeballing.
 *
 * Note that a bottom sheet paints here too: on skiko its dialog layer composes into the same
 * scene, so `ImageComposeScene` picks it up. What it does *not* do is stay inside the box that
 * launched it — see `SheetRegionContainmentTest`, which is where that difference is pinned down.
 */
@OptIn(ExperimentalComposeUiApi::class)
class AdaptiveSheetRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `a wide short region draws the sheet down its trailing edge`() {
    render("adaptive-sheet-side", width = 914, height = 411)
  }

  @Test
  fun `the shortest landscape phone gets the same sheet`() {
    render("adaptive-sheet-side-short", width = 780, height = 360)
  }

  @Test
  fun `a narrow short region gets one too, covering more of it`() {
    render("adaptive-sheet-side-narrow", width = 480, height = 360)
  }

  /**
   * The width follows the content, the way a bottom sheet's height does. Content that asks for
   * the room gets it up to the cap; content that wants less makes a narrower sheet.
   */
  @Test
  fun `the sheet is as wide as its content asks for`() {
    val roomy = sheetLeftEdge(image(914, 411, contentWidth = null))
    val slim = sheetLeftEdge(image(914, 411, contentWidth = 200))

    // Further from the left edge means a narrower sheet.
    assertThat(slim).isGreaterThan(roomy)
  }

  /**
   * The sheet can be dragged back towards the edge to close it, and the handle is what says so —
   * the bottom sheet's affordance turned ninety degrees. A column just inside the sheet's leading
   * edge runs straight through it, so it sees the pill and the surface behind it.
   */
  @Test
  fun `the sheet carries a drag handle near its leading edge`() {
    // Found rather than assumed, so the test says "there is a handle in the margin" instead of
    // restating whatever inset the sheet happens to use.
    assertThat(handleOffsetFromLeadingEdge(914, 411)).isLessThan(HandleSearchWidth)
    assertThat(handleOffsetFromLeadingEdge(480, 360)).isLessThan(HandleSearchWidth)
  }

  @Test
  fun `the sheet actually draws something over the content`() {
    assertThat(bytes(914, 411, sheet = true)).isNotEqualTo(bytes(914, 411, sheet = false))
    assertThat(bytes(480, 360, sheet = true)).isNotEqualTo(bytes(480, 360, sheet = false))
  }

  /**
   * The x of the sheet's leading edge, found by scanning the middle row for where the scrim gives
   * way to the sheet's surface. Colour-agnostic: the theme owns the actual values.
   */
  private fun sheetLeftEdge(image: Image): Int {
    val pixels = image.peekPixels()!!
    val y = image.height / 2
    val scrim = pixels.getColor(0, y)
    return (0 until image.width).first { x -> pixels.getColor(x, y) != scrim }
  }

  /**
   * How far in from the sheet's leading edge the drag handle sits, in pixels: the first column
   * that is not a single flat colour down the middle of the sheet. Returns [HandleSearchWidth] if
   * there is no such column, which fails the caller's bound.
   */
  private fun handleOffsetFromLeadingEdge(width: Int, height: Int): Int {
    val image = image(width, height)
    val pixels = image.peekPixels()!!
    val left = sheetLeftEdge(image)

    val top = image.height / 4
    val bottom = image.height * 3 / 4
    return (0 until HandleSearchWidth).firstOrNull { offset ->
      val x = left + offset
      (top until bottom).map { y -> pixels.getColor(x, y) }.distinct().size > 1
    } ?: HandleSearchWidth
  }

  private fun render(name: String, width: Int, height: Int) {
    val bytes = bytes(width, height, sheet = true)
    File(renders, "$name.png").writeBytes(bytes)
    println("rendered ${File(renders, "$name.png").absolutePath}")
    assertThat(bytes.size).isGreaterThan(1_000)
  }

  private fun bytes(width: Int, height: Int, sheet: Boolean): ByteArray =
    image(width, height, sheet = sheet).encodeToData(EncodedImageFormat.PNG)!!.bytes

  private fun image(
    width: Int,
    height: Int,
    sheet: Boolean = true,
    contentWidth: Int? = null,
  ): Image {
    val sizeClass = WindowSizeClass.BREAKPOINTS_V2
      .computeWindowSizeClass(width.toFloat(), height.toFloat())

    return ImageComposeScene(
      width = width * 2,
      height = height * 2,
      density = Density(2f),
      content = { Host(sizeClass, sheet, contentWidth) },
    ).use { scene ->
      // The overlay is shown from a LaunchedEffect and animates in, so let the scene settle.
      repeat(10) { frame -> scene.render(frame * 100_000_000L) }
      scene.render(nanoTime = 3_000_000_000L)
    }
  }

  @Composable
  private fun Host(sizeClass: WindowSizeClass, sheet: Boolean, contentWidth: Int?) {
    CompositionLocalProvider(LocalWindowSizeClass provides sizeClass) {
      CampfireTheme(useDarkColors = false) {
        val overlayHost = rememberOverlayHost()
        ContentWithOverlays(overlayHost = overlayHost, modifier = Modifier.fillMaxSize()) {
          Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Text("behind the sheet", Modifier.padding(24.dp))
          }
        }

        if (sheet) {
          LaunchedEffect(Unit) {
            overlayHost.show(
              AdaptiveSheetOverlay<Unit, Unit>(
                model = Unit,
                onDismiss = { },
              ) { _, _ ->
                Column(
                  // Null asks for the room, which is what a list does; a number stands in for
                  // content that wants less than the sheet would allow.
                  modifier = contentWidth?.let { Modifier.width(it.dp) } ?: Modifier.fillMaxWidth(),
                ) {
                  Text("Chapters", Modifier.padding(16.dp))
                  repeat(6) {
                    Box(
                      Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    )
                  }
                }
              },
            )
          }
        }
      }
    }
  }

  private companion object {
    /** How far in from the sheet's edge a handle may sit and still count as being in the margin. */
    const val HandleSearchWidth = 48
  }
}
