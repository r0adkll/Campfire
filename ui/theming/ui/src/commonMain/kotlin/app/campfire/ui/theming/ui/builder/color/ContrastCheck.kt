// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.color

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.campfire.common.compose.extensions.asHct
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.api.colorFor
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
 * Each pair is taken from a place the app theme reaches, so a failing check names something a
 * user would notice. Book details and the player take their colors from the cover by default,
 * so none of these depend on them.
 */
enum class ContrastCheck(
  val background: ThemeRole,
  val foreground: ThemeRole,
  val minimumRatio: Double,
) {
  /** Titles and text on screen backgrounds, such as Home's shelf names and settings. */
  ScreenText(ThemeRole.Surface, ThemeRole.OnSurface, TEXT_CONTRAST),

  /** Titles and authors on book, series and author cards. */
  CardText(ThemeRole.SurfaceContainerHigh, ThemeRole.OnSurface, TEXT_CONTRAST),

  /** The search bar's prompt and the message in dialogs. */
  SupportingText(ThemeRole.SurfaceContainerHigh, ThemeRole.OnSurfaceVariant, TEXT_CONTRAST),

  /** Dialog buttons such as Cancel and Save. */
  DialogButtons(ThemeRole.SurfaceContainerHigh, ThemeRole.Primary, TEXT_CONTRAST),

  /** Settings section headers and the active filter icon. */
  Headers(ThemeRole.Surface, ThemeRole.Primary, TEXT_CONTRAST),

  /** Switches that are on, and filled buttons such as Install. */
  Buttons(ThemeRole.Primary, ThemeRole.OnPrimary, TEXT_CONTRAST),

  /** Listening progress along the bottom of covers. */
  ProgressBars(ThemeRole.SurfaceContainerHighest, ThemeRole.Primary, NON_TEXT_CONTRAST),

  /** The account card at the top of the navigation drawer, and the update card. */
  AccountCard(ThemeRole.PrimaryContainer, ThemeRole.OnPrimaryContainer, TEXT_CONTRAST),

  /** The selected tab's icon, the selected drawer item and the mini player. */
  SelectedItems(ThemeRole.SecondaryContainer, ThemeRole.OnSecondaryContainer, TEXT_CONTRAST),

  /** The selected tab's name in the navigation bar. */
  SelectedTabName(ThemeRole.SurfaceContainer, ThemeRole.Secondary, TEXT_CONTRAST),

  /** The other tabs in the navigation bar. */
  NavigationBar(ThemeRole.SurfaceContainer, ThemeRole.OnSurfaceVariant, TEXT_CONTRAST),

  /** The other items in the navigation drawer. */
  DrawerItems(ThemeRole.SurfaceContainerLow, ThemeRole.OnSurfaceVariant, TEXT_CONTRAST),

  /** Working offline, in the drawer and Connection settings, and podcast episode numbers. */
  OfflineStatus(ThemeRole.TertiaryContainer, ThemeRole.OnTertiaryContainer, TEXT_CONTRAST),

  /** Delete buttons and field errors in dialogs. */
  ErrorText(ThemeRole.SurfaceContainerHigh, ThemeRole.Error, TEXT_CONTRAST),

  /** Warnings such as a cover that won't load. */
  Warnings(ThemeRole.ErrorContainer, ThemeRole.OnErrorContainer, TEXT_CONTRAST),

  /** Switches that are off, and text field borders. */
  Borders(ThemeRole.Surface, ThemeRole.Outline, NON_TEXT_CONTRAST),

  /** Snackbars such as "Back online", and tooltips. */
  Snackbars(ThemeRole.InverseSurface, ThemeRole.InverseOnSurface, TEXT_CONTRAST),
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
