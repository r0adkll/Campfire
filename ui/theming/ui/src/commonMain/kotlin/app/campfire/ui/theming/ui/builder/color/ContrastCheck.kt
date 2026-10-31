// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.color

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.campfire.common.compose.extensions.asHct
import com.r0adkll.swatchbuckler.color.contrast.Contrast
import com.r0adkll.swatchbuckler.color.hct.Hct
import com.r0adkll.swatchbuckler.color.utils.ColorUtils
import kotlin.math.roundToInt

/** WCAG AA minimum for normal-size text. */
internal const val TEXT_CONTRAST = 4.5

/** WCAG minimum for non-text parts such as bars, borders and switch tracks. */
internal const val NON_TEXT_CONTRAST = 3.0

/**
 * A pair of colors Campfire draws together, and the contrast that pair needs to stay legible.
 * Each pair comes from a real screen, so a failing check names a place in the app a user would
 * notice.
 */
enum class ContrastCheck(
  val background: ThemeRole,
  val foreground: ThemeRole,
  val minimumRatio: Double,
) {
  /** Screen backgrounds, titles and body text. */
  ScreenText(ThemeRole.Surface, ThemeRole.OnSurface, TEXT_CONTRAST),

  /** Author names and durations on book cards. */
  CardText(ThemeRole.SurfaceContainerHigh, ThemeRole.OnSurfaceVariant, TEXT_CONTRAST),

  /** Dialog buttons and links on cards. */
  Links(ThemeRole.SurfaceContainerHigh, ThemeRole.Primary, TEXT_CONTRAST),

  /** Continue listening, the big play button, switches. */
  PlayButtons(ThemeRole.Primary, ThemeRole.OnPrimary, TEXT_CONTRAST),

  /** Listening progress under covers and the seek bar. */
  ProgressBars(ThemeRole.PrimaryContainer, ThemeRole.Primary, NON_TEXT_CONTRAST),

  /** The user's avatar and other prominent containers. */
  Highlights(ThemeRole.PrimaryContainer, ThemeRole.OnPrimaryContainer, TEXT_CONTRAST),

  /** The current tab, the current chapter and the mini player button. */
  SelectedItems(ThemeRole.SecondaryContainer, ThemeRole.OnSecondaryContainer, TEXT_CONTRAST),

  /** Durations and "new" counts. */
  Badges(ThemeRole.TertiaryContainer, ThemeRole.OnTertiaryContainer, TEXT_CONTRAST),

  /** The chapter and time left under the mini player's title. */
  MiniPlayer(ThemeRole.SurfaceContainerHighest, ThemeRole.OnSurfaceVariant, TEXT_CONTRAST),

  /** Unselected tabs in the navigation bar. */
  NavigationBar(ThemeRole.SurfaceContainer, ThemeRole.OnSurfaceVariant, TEXT_CONTRAST),

  /** Banners such as "Can't reach your server". */
  ErrorBanners(ThemeRole.ErrorContainer, ThemeRole.OnErrorContainer, TEXT_CONTRAST),

  /** Field errors and destructive buttons. */
  ErrorText(ThemeRole.Surface, ThemeRole.Error, TEXT_CONTRAST),

  /** Text fields, outlined chips and switches that are off. */
  Borders(ThemeRole.Surface, ThemeRole.Outline, NON_TEXT_CONTRAST),

  /** Snackbar messages such as "Marked as finished". */
  Snackbars(ThemeRole.InverseSurface, ThemeRole.InverseOnSurface, TEXT_CONTRAST),

  /** Snackbar actions such as "Undo". */
  SnackbarActions(ThemeRole.InverseSurface, ThemeRole.InversePrimary, TEXT_CONTRAST),
}

data class ContrastResult(
  val check: ContrastCheck,
  val ratio: Double,
) {
  val passes: Boolean get() = ratio >= check.minimumRatio
}

internal fun ColorScheme.contrastResults(): List<ContrastResult> = ContrastCheck.entries.map { check ->
  ContrastResult(
    check = check,
    ratio = contrastRatio(colorFor(check.foreground), colorFor(check.background)),
  )
}

/**
 * The WCAG contrast ratio between two opaque colors, from 1 (identical luminance) to 21
 * (black on white). The order of the arguments doesn't matter.
 */
internal fun contrastRatio(a: Color, b: Color): Double = Contrast.ratioOfTones(
  ColorUtils.lstarFromArgb(a.toArgb()),
  ColorUtils.lstarFromArgb(b.toArgb()),
)

/**
 * Returns a version of [foreground] that reaches [minimumRatio] against [background], keeping its
 * hue and chroma and moving its tone as little as possible. Returns [foreground] unchanged when it
 * already passes, and black or white, whichever contrasts more, when no tone of its hue gets there.
 */
internal fun fixForeground(
  foreground: Color,
  background: Color,
  minimumRatio: Double,
): Color {
  if (contrastRatio(foreground, background) >= minimumRatio) return foreground

  val hct = foreground.asHct()
  val startTone = hct.tone.roundToInt()
  for (delta in 1..MAX_TONE) {
    for (tone in intArrayOf(startTone + delta, startTone - delta)) {
      if (tone !in 0..MAX_TONE) continue
      val candidate = Color(Hct.from(hct.hue, hct.chroma, tone.toDouble()).toInt())
      if (contrastRatio(candidate, background) >= minimumRatio) return candidate
    }
  }

  return if (contrastRatio(Color.Black, background) >= contrastRatio(Color.White, background)) {
    Color.Black
  } else {
    Color.White
  }
}

private const val MAX_TONE = 100
