// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.theme.color

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import app.campfire.common.compose.extensions.asHct
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.theme.alt.AltBlueColorPalette
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import com.r0adkll.swatchbuckler.color.contrast.Contrast
import kotlin.math.abs
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CampfireColorSchemeTest {

  private val indigo = Color(0xFF3F51B5)

  @Test
  fun `harmonizing keeps every pair as readable, to within rounding`() {
    listOf(lightCampfireColorScheme(), darkCampfireColorScheme()).forEach { scheme ->
      val harmonized = scheme.harmonizedWith(indigo)
      scheme.pairs().zip(harmonized.pairs()).forEach { (before, after) ->
        val original = contrast(before.first, before.second)
        // Lightness is kept, but each color still rounds to 8-bit RGB.
        assertThat(contrast(after.first, after.second)).isCloseTo(original, original * 0.01)
      }
    }
  }

  @Test
  fun `success and loading lean toward the theme's hue by at most 15 degrees`() {
    val scheme = lightCampfireColorScheme()
    val harmonized = scheme.harmonizedWith(indigo)

    listOf(scheme.success to harmonized.success, scheme.loading to harmonized.loading).forEach { (before, after) ->
      val moved = hueDistance(before, after)
      assertThat(moved).isBetween(1.0, 15.5)
      // Closer to the theme's hue than it started.
      assertThat(hueDistance(after, indigo) < hueDistance(before, indigo)).isEqualTo(true)
    }
  }

  @Test
  fun `a theme of the same hue leaves them where they were`() {
    val scheme = lightCampfireColorScheme()

    val harmonized = scheme.harmonizedWith(scheme.success)

    assertThat(hueDistance(scheme.success, harmonized.success)).isCloseTo(0.0, 1.0)
  }

  @Test
  fun `the app theme provides success and loading colors harmonized with its primary`() = runComposeUiTest {
    var provided: CampfireColorScheme? = null
    var primary = Color.Unspecified
    setContent {
      CampfireTheme(colorPalette = AltBlueColorPalette, useDarkColors = true) {
        provided = CampfireTheme.colorScheme
        primary = MaterialTheme.colorScheme.primary
      }
    }
    waitForIdle()

    val expected = darkCampfireColorScheme().harmonizedWith(primary)
    assertThat(provided!!.pairs()).isEqualTo(expected.pairs())
  }

  private fun CampfireColorScheme.pairs() = listOf(
    success to onSuccess,
    successContainer to onSuccessContainer,
    loading to onLoading,
  )

  private fun contrast(a: Color, b: Color): Double = Contrast.ratioOfTones(a.asHct().tone, b.asHct().tone)

  private fun hueDistance(a: Color, b: Color): Double {
    val difference = abs(a.asHct().hue - b.asHct().hue) % 360.0
    return if (difference > 180.0) 360.0 - difference else difference
  }
}
