// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.ui.theming.api.colorScheme
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import app.campfire.ui.theming.ui.builder.composables.ColorPickerPanel
import app.campfire.ui.theming.ui.preview.CoverThemingOff
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.campfire.ui.theming.ui.preview.installTestCovers
import app.campfire.ui.theming.ui.preview.previewLibrary
import app.campfire.ui.theming.ui.preview.resetTestCovers
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the theme builder off-screen at phone and tablet widths, plus its color picker, and
 * writes PNGs to `build/renders` for eyeballing; the assertions only guard that each render paints.
 * The preview shows a test library with flat-color covers and cover theming off, so every part
 * of the theme is on show.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ThemeBuilderRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }
  private val colors = CustomThemeColors(Color(0xFFC0532D), Variant.EXPRESSIVE, tertiaryColor = Color(0xFF0E7490))
  private val content = PreviewContent(previewLibrary(), CoverThemingOff)

  @BeforeTest
  fun installCovers() = installTestCovers()

  @AfterTest
  fun tearDown() = resetTestCovers()

  @Test
  fun `phone light`() {
    render("theme-builder-phone-light", width = 412, height = 915) { Builder(dark = false) }
  }

  @Test
  fun `phone dark`() {
    render("theme-builder-phone-dark", width = 412, height = 915) { Builder(dark = true) }
  }

  @Test
  fun `two pane`() {
    render("theme-builder-two-pane", width = 1280, height = 800) { Builder(dark = false) }
  }

  @Test
  fun `color picker`() {
    render("theme-builder-picker", width = 412, height = 560) {
      CampfireTheme(useDarkColors = false) {
        val state = rememberSampleThemeBuilderState(colors)
        MaterialExpressiveTheme(colorScheme = colorScheme(state.theme, false)) {
          ColorPickerPanel(
            key = PaletteKey.ThirdAccent,
            initialColor = Color(0xFF0E7490),
            seedColor = state.seedColor,
            canReset = true,
            onColorChange = {},
            onResetClick = {},
            onDoneClick = {},
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.surfaceContainer),
          )
        }
      }
    }
  }

  @Composable
  private fun Builder(dark: Boolean) {
    CampfireTheme(useDarkColors = dark) {
      CompositionLocalProvider(
        LocalUseDarkColors provides dark,
        LocalWindowSizeClass provides currentWindowSizeClass(),
        LocalContentLayout provides ContentLayout.Root,
      ) {
        ThemeBuilder(rememberSampleThemeBuilderState(colors, previewContent = content))
      }
    }
  }

  private fun render(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
    ImageComposeScene(
      width = width * 2,
      height = height * 2,
      density = Density(2f),
      coroutineContext = Dispatchers.Unconfined,
      content = content,
    ).use { scene ->
      // The first frame starts the cover requests; the second draws them.
      scene.render()
      val bytes = scene.render(nanoTime = 2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }
}
