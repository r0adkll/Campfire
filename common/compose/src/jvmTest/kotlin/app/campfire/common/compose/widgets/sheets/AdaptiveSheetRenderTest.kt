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
import assertk.assertions.isNotEqualTo
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import java.io.File
import kotlin.test.Test
import org.jetbrains.skia.EncodedImageFormat

/**
 * The side panel and the centred card, drawn over stand-in content at the sizes that select them.
 * PNGs land in `build/renders` for eyeballing.
 *
 * Note that a bottom sheet does paint here too: on skiko its dialog layer composes into the same
 * scene, so `ImageComposeScene` picks it up. What it does *not* do is stay inside the box that
 * launched it — see `SheetRegionContainmentTest`, which is where that difference is pinned down.
 */
@OptIn(ExperimentalComposeUiApi::class)
class AdaptiveSheetRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @Test
  fun `a wide short region draws the panel down its trailing edge`() {
    render("adaptive-sheet-side", width = 914, height = 411)
  }

  @Test
  fun `the shortest landscape phone gets the same panel`() {
    render("adaptive-sheet-side-short", width = 780, height = 360)
  }

  @Test
  fun `a region too narrow for a panel gets the centred card`() {
    render("adaptive-sheet-dialog", width = 480, height = 360)
  }

  @Test
  fun `each presentation actually draws something over the content`() {
    assertThat(bytes(914, 411, sheet = true)).isNotEqualTo(bytes(914, 411, sheet = false))
    assertThat(bytes(480, 360, sheet = true)).isNotEqualTo(bytes(480, 360, sheet = false))
  }

  private fun render(name: String, width: Int, height: Int) {
    val bytes = bytes(width, height, sheet = true)
    File(renders, "$name.png").writeBytes(bytes)
    println("rendered ${File(renders, "$name.png").absolutePath}")
    assertThat(bytes.size).isGreaterThan(1_000)
  }

  private fun bytes(width: Int, height: Int, sheet: Boolean): ByteArray {
    val sizeClass = WindowSizeClass.BREAKPOINTS_V2
      .computeWindowSizeClass(width.toFloat(), height.toFloat())

    return ImageComposeScene(
      width = width * 2,
      height = height * 2,
      density = Density(2f),
      content = { Host(sizeClass, sheet) },
    ).use { scene ->
      // The overlay is shown from a LaunchedEffect and animates in, so let the scene settle.
      repeat(8) { frame -> scene.render(frame * 100_000_000L) }
      scene.render(nanoTime = 2_000_000_000L)
        .encodeToData(EncodedImageFormat.PNG)!!
        .bytes
    }
  }

  @Composable
  private fun Host(sizeClass: WindowSizeClass, sheet: Boolean) {
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
                Column(Modifier.fillMaxWidth()) {
                  Text("Chapters", Modifier.padding(16.dp))
                  repeat(6) { row ->
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
}
