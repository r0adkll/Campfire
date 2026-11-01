// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.theme.color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.FatCheck
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.theme.ColorPalette
import app.campfire.common.compose.theme.alt.AltBlueColorPalette
import app.campfire.common.compose.theme.alt.AltGreenColorPalette
import app.campfire.common.compose.theme.alt.AltOrangeColorPalette
import app.campfire.common.compose.theme.alt.AltPurpleColorPalette
import app.campfire.common.compose.theme.alt.AltYellowColorPalette
import app.campfire.common.compose.theme.tents.RedColorPalette
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the success and loading colors for each built-in theme, fixed and then harmonized with
 * the theme, in light and dark, and writes PNGs to `build/renders` for eyeballing.
 */
@OptIn(ExperimentalComposeUiApi::class)
class CampfireColorSchemeRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  private val themes = listOf(
    "Tent" to RedColorPalette,
    "Rucksack" to AltYellowColorPalette,
    "Water bottle" to AltBlueColorPalette,
    "Forest" to AltGreenColorPalette,
    "Mountain" to AltPurpleColorPalette,
    "Life float" to AltOrangeColorPalette,
  )

  @Test
  fun `success and loading colors, light`() = render("campfire-colors-light", dark = false)

  @Test
  fun `success and loading colors, dark`() = render("campfire-colors-dark", dark = true)

  private fun render(name: String, dark: Boolean) {
    ImageComposeScene(
      width = 520 * 2,
      height = (48 + themes.size * 56) * 2,
      density = Density(2f),
      coroutineContext = Dispatchers.Unconfined,
    ) {
      Column(Modifier.fillMaxSize().background(if (dark) Color(0xFF121212) else Color.White).padding(8.dp)) {
        Row(Modifier.padding(start = 112.dp, bottom = 8.dp)) {
          Text("Fixed", Modifier.width(200.dp), color = if (dark) Color.White else Color.Black)
          Text("Harmonized", color = if (dark) Color.White else Color.Black)
        }
        themes.forEach { (label, palette) -> ThemeRow(label, palette, dark) }
      }
    }.use { scene ->
      val bytes = scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertTrue(bytes.size > 1_000)
    }
  }

  @Composable
  private fun ThemeRow(label: String, palette: ColorPalette, dark: Boolean) {
    CampfireTheme(colorPalette = palette, useDarkColors = dark) {
      Row(
        modifier = Modifier
          .padding(vertical = 4.dp)
          .background(MaterialTheme.colorScheme.surface)
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(Modifier.width(104.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(16.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
          Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
          )
        }
        Swatches(campfireColorScheme(dark), Modifier.width(200.dp))
        Swatches(CampfireTheme.colorScheme)
      }
    }
  }

  @Composable
  private fun Swatches(scheme: CampfireColorScheme, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Swatch(scheme.success, scheme.onSuccess)
      Swatch(scheme.successContainer, scheme.onSuccessContainer)
      Swatch(scheme.loading, scheme.onLoading)
    }
  }

  @Composable
  private fun Swatch(container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
      modifier = modifier
        .size(32.dp)
        .background(container, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(CampfireIcons.Rounded.FatCheck, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
    }
  }
}
