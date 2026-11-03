// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.core.coroutines.LoadState
import app.campfire.settings.api.ThemeMode
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.campfire.ui.theming.ui.preview.installTestCovers
import app.campfire.ui.theming.ui.preview.previewLibrary
import app.campfire.ui.theming.ui.preview.resetTestCovers
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat

/** Renders the theme picker off-screen to `build/renders` for eyeballing. */
@OptIn(ExperimentalComposeUiApi::class)
class ThemePickerRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  @BeforeTest
  fun installCovers() = installTestCovers()

  @AfterTest
  fun tearDown() = resetTestCovers()

  @Test
  fun `phone light`() {
    render("theme-picker-phone", width = 412, height = 915) { Picker(dark = false) }
  }

  @Test
  fun `phone dark`() {
    render("theme-picker-phone-dark", width = 412, height = 915) { Picker(dark = true) }
  }

  @Test
  fun `small phone`() {
    render("theme-picker-small-phone", width = 360, height = 640) { Picker(dark = false) }
  }

  @Test
  fun `phone with larger text`() {
    render("theme-picker-phone-large-text", width = 412, height = 915, fontScale = 1.3f) { Picker(dark = false) }
  }

  @Test
  fun `tablet`() {
    render("theme-picker-tablet", width = 1280, height = 800) { Picker(dark = false) }
  }

  @Composable
  private fun Picker(dark: Boolean) {
    CampfireTheme(useDarkColors = dark) {
      CompositionLocalProvider(
        LocalUseDarkColors provides dark,
        LocalWindowSizeClass provides currentWindowSizeClass(),
        LocalContentLayout provides ContentLayout.Root,
      ) {
        ThemePicker(
          ThemePickerUiState(
            currentTheme = AppTheme.Fixed.Tent,
            themeMode = if (dark) ThemeMode.DARK else ThemeMode.SYSTEM,
            builtInThemes = BuiltInThemes,
            customThemes = LoadState.Loaded(listOf(customTheme(id = "ember", name = "Ember"), aiTheme())),
            aiThemeBuilderAvailable = true,
            previewContent = PreviewContent(previewLibrary()),
            eventSink = {},
          ),
        )
      }
    }
  }

  private fun render(
    name: String,
    width: Int,
    height: Int,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
  ) {
    ImageComposeScene(
      width = width * 2,
      height = height * 2,
      density = Density(2f, fontScale),
      coroutineContext = Dispatchers.Unconfined,
      content = content,
    ).use { scene ->
      // Covers load over a few frames.
      repeat(4) { scene.render(nanoTime = it * 500_000_000L) }
      val bytes = scene.render(nanoTime = 2_500_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }
}
