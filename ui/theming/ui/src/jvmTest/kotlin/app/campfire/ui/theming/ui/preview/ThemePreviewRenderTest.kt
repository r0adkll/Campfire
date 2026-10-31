// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders every preview scene side by side off-screen and writes PNGs to `build/renders` for
 * eyeballing; the assertions only guard that each render paints.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ThemePreviewRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }
  private val palette = CustomThemeColors(Color(0xFFC0532D), Variant.EXPRESSIVE).toColorPalette()

  @Test
  fun `scenes light`() {
    render("theme-preview-light") { Scenes(dark = false) }
  }

  @Test
  fun `scenes dark`() {
    render("theme-preview-dark") { Scenes(dark = true) }
  }

  @Test
  fun `scenes with every check flagged`() {
    render("theme-preview-flagged") {
      Scenes(dark = false, targets = ThemeTargets(failing = ContrastCheck.entries.toSet()))
    }
  }

  @Composable
  private fun Scenes(
    dark: Boolean,
    targets: ThemeTargets = ThemeTargets.None,
  ) {
    CampfireTheme(colorPalette = palette, useDarkColors = dark) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.surfaceContainer)
          .padding(GAP.dp),
        horizontalArrangement = Arrangement.spacedBy(GAP.dp),
      ) {
        PreviewScene.entries.forEach { scene ->
          ThemePreviewContent(
            scene = scene,
            targets = targets,
            modifier = Modifier.size(width = SCENE_WIDTH.dp, height = SCENE_HEIGHT.dp),
          )
        }
      }
    }
  }

  private fun render(name: String, content: @Composable () -> Unit) {
    val width = PreviewScene.entries.size * (SCENE_WIDTH + GAP) + GAP
    val height = SCENE_HEIGHT + GAP * 2
    ImageComposeScene(width = width * 2, height = height * 2, density = Density(2f), content = content).use { scene ->
      val bytes = scene.render(nanoTime = 1_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }

  private companion object {
    const val SCENE_WIDTH = 340
    const val SCENE_HEIGHT = 680
    const val GAP = 16
  }
}
