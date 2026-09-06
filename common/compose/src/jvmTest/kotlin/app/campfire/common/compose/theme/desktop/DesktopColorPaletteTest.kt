// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.theme.desktop

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import app.campfire.common.compose.extensions.asHct
import app.campfire.common.compose.theme.alt.AltBlueColorPalette
import app.campfire.common.compose.theme.alt.AltGreenColorPalette
import app.campfire.common.compose.theme.alt.AltOrangeColorPalette
import app.campfire.common.compose.theme.alt.AltPurpleColorPalette
import app.campfire.common.compose.theme.alt.AltYellowColorPalette
import app.campfire.common.compose.theme.tents.RedColorPalette
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotEqualTo
import com.r0adkll.swatchbuckler.color.contrast.Contrast
import com.r0adkll.swatchbuckler.color.utils.MathUtils
import kotlin.test.Test

class DesktopColorPaletteTest {

  private val builtIns = mapOf(
    "Red" to (RedColorPalette to RedDesktopColorPalette),
    "Yellow" to (AltYellowColorPalette to AltYellowDesktopColorPalette),
    "Blue" to (AltBlueColorPalette to AltBlueDesktopColorPalette),
    "Green" to (AltGreenColorPalette to AltGreenDesktopColorPalette),
    "Purple" to (AltPurpleColorPalette to AltPurpleDesktopColorPalette),
    "Orange" to (AltOrangeColorPalette to AltOrangeDesktopColorPalette),
  )

  @Test
  fun `neutral roles stay close to grey in both modes`() {
    assertAll {
      builtIns.forEach { (name, palettes) ->
        val (_, desktop) = palettes
        listOf("light" to desktop.lightColorScheme, "dark" to desktop.darkColorScheme).forEach { (mode, scheme) ->
          scheme.neutralRoles().forEach { (role, color) ->
            assertThat(color.asHct().chroma, "$name $mode $role chroma").isLessThan(DesktopNeutralChroma * 2 + 1)
          }
        }
      }
    }
  }

  @Test
  fun `neutral roles keep a cool cast`() {
    assertAll {
      builtIns.forEach { (name, palettes) ->
        val (_, desktop) = palettes
        listOf("light" to desktop.lightColorScheme, "dark" to desktop.darkColorScheme).forEach { (mode, scheme) ->
          scheme.neutralRoles().forEach { (role, color) ->
            val hct = color.asHct()
            // Tones pinned to black or white clip to the sRGB gamut and carry no meaningful hue.
            if (hct.tone < 99.0 && hct.chroma > 2.0) {
              assertThat(hct.hue, "$name $mode $role hue").isBetween(DesktopNeutralHue - 25, DesktopNeutralHue + 25)
            }
          }
        }
      }
    }
  }

  @Test
  fun `primary keeps the accent hue of the source palette`() {
    assertAll {
      builtIns.forEach { (name, palettes) ->
        val (source, desktop) = palettes
        val seedHue = source.lightColorScheme.primary.asHct().hue
        val desktopHue = desktop.lightColorScheme.primary.asHct().hue
        assertThat(
          MathUtils.differenceDegrees(seedHue, desktopHue),
          "$name primary hue drift",
        ).isLessThan(5.0)
        assertThat(desktop.lightColorScheme.primary.asHct().chroma, "$name primary chroma").isGreaterThan(20.0)
      }
    }
  }

  @Test
  fun `light and dark surfaces sit at the expected tones`() {
    assertAll {
      builtIns.forEach { (name, palettes) ->
        val (_, desktop) = palettes
        assertThat(desktop.lightColorScheme.surface.asHct().tone, "$name light surface tone").isGreaterThan(95.0)
        assertThat(desktop.darkColorScheme.surface.asHct().tone, "$name dark surface tone").isLessThan(10.0)
        assertThat(desktop.lightColorScheme.onSurface.asHct().tone, "$name light onSurface tone").isLessThan(15.0)
        assertThat(desktop.darkColorScheme.onSurface.asHct().tone, "$name dark onSurface tone").isGreaterThan(85.0)
      }
    }
  }

  @Test
  fun `tonal elevation tints towards grey rather than the accent`() {
    assertAll {
      builtIns.forEach { (name, palettes) ->
        val (_, desktop) = palettes
        listOf("light" to desktop.lightColorScheme, "dark" to desktop.darkColorScheme).forEach { (mode, scheme) ->
          assertThat(scheme.surfaceTint, "$name $mode surfaceTint").isNotEqualTo(scheme.primary)
          assertThat(scheme.surfaceTint.asHct().chroma, "$name $mode surfaceTint chroma").isLessThan(
            DesktopNeutralChroma + 1,
          )
        }
      }
    }
  }

  @Test
  fun `built-ins use the vibrant punchy accent`() {
    assertAll {
      builtIns.forEach { (name, palettes) ->
        val (source, desktop) = palettes
        val expected = desktopColorPalette(seed = source.lightColorScheme.primary, accent = DesktopAccent.VibrantPunchy)
        assertThat(desktop.lightColorScheme.primary, "$name light primary").isEqualTo(expected.lightColorScheme.primary)
        assertThat(desktop.darkColorScheme.primary, "$name dark primary").isEqualTo(expected.darkColorScheme.primary)
        assertThat(desktop.lightColorScheme.tertiary, "$name light tertiary").isEqualTo(
          expected.lightColorScheme.tertiary,
        )
      }
    }
  }

  @Test
  fun `punchy accent raises chroma to the floor`() {
    assertAll {
      builtIns.forEach { (name, palettes) ->
        val (_, desktop) = palettes
        listOf("light" to desktop.lightColorScheme, "dark" to desktop.darkColorScheme).forEach { (mode, scheme) ->
          // Yellow and orange hues cannot hold the full floor at these tones; the gamut clips them.
          val floor = if (name == "Yellow" || name == "Orange") 30.0 else DesktopAccent.Punchy.minChroma - 12.0
          assertThat(scheme.primary.asHct().chroma, "$name $mode primary chroma").isGreaterThan(floor)
        }
      }
    }
  }

  @Test
  fun `accent roles keep readable contrast`() {
    assertAll {
      builtIns.forEach { (name, palettes) ->
        val (_, desktop) = palettes
        listOf("light" to desktop.lightColorScheme, "dark" to desktop.darkColorScheme).forEach { (mode, scheme) ->
          assertThat(contrast(scheme.onPrimary, scheme.primary), "$name $mode onPrimary/primary")
            .isGreaterThanOrEqualTo(4.5)
          assertThat(contrast(scheme.onPrimaryContainer, scheme.primaryContainer), "$name $mode onPrimaryContainer")
            .isGreaterThanOrEqualTo(4.5)
          assertThat(contrast(scheme.primary, scheme.surface), "$name $mode primary/surface")
            .isGreaterThanOrEqualTo(3.0)
          assertThat(contrast(scheme.primary, scheme.surfaceContainerHighest), "$name $mode primary/containerHighest")
            .isGreaterThanOrEqualTo(3.0)
        }
      }
    }
  }

  @Test
  fun `every accent preset keeps readable contrast`() {
    val presets = mapOf(
      "Material" to DesktopAccent.Material,
      "Punchy" to DesktopAccent.Punchy,
      "Vibrant" to DesktopAccent.Vibrant,
      "VibrantPunchy" to DesktopAccent.VibrantPunchy,
    )
    assertAll {
      presets.forEach { (presetName, preset) ->
        builtIns.forEach { (name, palettes) ->
          val (source, _) = palettes
          val desktop = desktopColorPalette(seed = source.lightColorScheme.primary, accent = preset)
          listOf("light" to desktop.lightColorScheme, "dark" to desktop.darkColorScheme).forEach { (mode, scheme) ->
            assertThat(contrast(scheme.onPrimary, scheme.primary), "$presetName $name $mode onPrimary/primary")
              .isGreaterThanOrEqualTo(4.5)
            assertThat(contrast(scheme.primary, scheme.surface), "$presetName $name $mode primary/surface")
              .isGreaterThanOrEqualTo(3.0)
          }
        }
      }
    }
  }

  @Test
  fun `material accent keeps stock tones`() {
    val stock = desktopColorPalette(seed = Color(0xFF904B3C), accent = DesktopAccent.Material)
    assertThat(stock.lightColorScheme.primary.asHct().tone).isBetween(38.0, 42.0)
    assertThat(stock.darkColorScheme.primary.asHct().tone).isBetween(78.0, 82.0)
  }

  @Test
  fun `custom neutral ramps are honoured`() {
    val warm = desktopColorPalette(seed = Color(0xFF904B3C), neutralHue = 70.0, neutralChroma = 6.0)
    // A mid tone keeps its requested chroma; near-white surfaces clip towards grey.
    val outline = warm.lightColorScheme.outline.asHct()
    assertThat(outline.hue).isBetween(45.0, 95.0)
    assertThat(outline.chroma).isBetween(6.0, 14.0)
  }

  private fun contrast(a: Color, b: Color): Double = Contrast.ratioOfTones(a.asHct().tone, b.asHct().tone)

  private fun ColorScheme.neutralRoles(): Map<String, Color> = mapOf(
    "background" to background,
    "onBackground" to onBackground,
    "surface" to surface,
    "onSurface" to onSurface,
    "surfaceVariant" to surfaceVariant,
    "onSurfaceVariant" to onSurfaceVariant,
    "surfaceDim" to surfaceDim,
    "surfaceBright" to surfaceBright,
    "surfaceContainerLowest" to surfaceContainerLowest,
    "surfaceContainerLow" to surfaceContainerLow,
    "surfaceContainer" to surfaceContainer,
    "surfaceContainerHigh" to surfaceContainerHigh,
    "surfaceContainerHighest" to surfaceContainerHighest,
    "inverseSurface" to inverseSurface,
    "inverseOnSurface" to inverseOnSurface,
    "outline" to outline,
    "outlineVariant" to outlineVariant,
  )
}
