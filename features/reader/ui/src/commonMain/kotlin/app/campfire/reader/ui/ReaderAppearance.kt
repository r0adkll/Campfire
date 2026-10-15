// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import app.campfire.reader.ui.web.ReaderStyle
import app.campfire.settings.api.ReaderFlow
import app.campfire.settings.api.ReaderFont
import app.campfire.settings.api.ReaderLayout
import app.campfire.settings.api.ReaderLayoutOverrides
import app.campfire.settings.api.ReaderMargin
import app.campfire.settings.api.ReaderPages
import app.campfire.settings.api.ReaderTheme

/** The page's colors for a [ReaderTheme]. */
@Immutable
data class ReaderPalette(
  val background: Color,
  val foreground: Color,
  val link: Color,
) {
  val isDark: Boolean get() = background.luminance() < 0.5f
}

@Composable
fun ReaderTheme.palette(): ReaderPalette = when (this) {
  ReaderTheme.FollowApp -> MaterialTheme.colorScheme.let {
    ReaderPalette(background = it.surface, foreground = it.onSurface, link = it.primary)
  }
  ReaderTheme.Light -> ReaderPalette(Color(0xFFFFFFFF), Color(0xFF1F1F1F), Color(0xFF1A5FB4))
  ReaderTheme.Sepia -> ReaderPalette(Color(0xFFF4ECD8), Color(0xFF5B4636), Color(0xFF8B4513))
  ReaderTheme.Dark -> ReaderPalette(Color(0xFF1E1E1E), Color(0xFFDADADA), Color(0xFF8AB4F8))
  ReaderTheme.Black -> ReaderPalette(Color(0xFF000000), Color(0xFFB8B8B8), Color(0xFF8AB4F8))
}

/**
 * The app's color scheme recolored to sit on [palette], so the reader's bars and panels match the
 * page rather than the app.
 */
fun ColorScheme.forPage(palette: ReaderPalette): ColorScheme {
  val tint = { amount: Float -> palette.foreground.copy(alpha = amount).compositeOver(palette.background) }
  return copy(
    primary = palette.link,
    onPrimary = palette.background,
    surface = palette.background,
    onSurface = palette.foreground,
    onSurfaceVariant = palette.foreground.copy(alpha = 0.72f).compositeOver(palette.background),
    surfaceContainerLowest = palette.background,
    surfaceContainerLow = tint(0.03f),
    surfaceContainer = tint(0.06f),
    surfaceContainerHigh = tint(0.10f),
    surfaceContainerHighest = tint(0.14f),
    secondaryContainer = tint(0.16f),
    onSecondaryContainer = palette.foreground,
    outline = palette.foreground.copy(alpha = 0.4f).compositeOver(palette.background),
    outlineVariant = tint(0.2f),
  )
}

/** The CSS font family list for [ReaderFont], or null to keep the book's own. */
private val ReaderFont.cssFamily: String?
  get() = when (this) {
    ReaderFont.Publisher -> null
    ReaderFont.Serif -> "serif"
    ReaderFont.Sans -> "sans-serif"
    ReaderFont.Literata -> "'Literata', serif"
    ReaderFont.AtkinsonHyperlegible -> "'Atkinson Hyperlegible Next', sans-serif"
    ReaderFont.OpenDyslexic -> "'OpenDyslexic', sans-serif"
  }

private val ReaderMargin.gapPercent: Int
  get() = when (this) {
    ReaderMargin.Narrow -> 4
    ReaderMargin.Normal -> 7
    ReaderMargin.Wide -> 12
  }

/**
 * How the page draws the book with [layout] in [palette]'s colors. [foldGap] is the gap, in
 * percent, that keeps two pages clear of a fold down the middle of the window: then two pages
 * show either side of it, unless the reader chose one page or scrolling.
 */
fun readerStyle(palette: ReaderPalette, layout: ReaderLayout, foldGap: Int? = null): ReaderStyle {
  val paged = layout.flow == ReaderFlow.Paged
  val splitOnFold = foldGap != null && paged && layout.pages != ReaderPages.Single
  return ReaderStyle(
    background = palette.background.toCss(),
    foreground = palette.foreground.toCss(),
    link = palette.link.toCss(),
    fontFamily = layout.font.cssFamily,
    fontScale = layout.fontScale,
    lineHeight = layout.lineHeight,
    justify = layout.justify,
    hyphenate = layout.hyphenate,
    gap = if (splitOnFold) maxOf(layout.margin.gapPercent, foldGap) else layout.margin.gapPercent,
    scrolled = !paged,
    columns = if (layout.pages == ReaderPages.Single) 1 else 2,
    portraitColumns = if (splitOnFold || layout.pages == ReaderPages.Two) 2 else 1,
  )
}

/** One change made in the appearance panel, applied to all books or to this one. */
sealed interface ReaderLayoutChange {
  data class Font(val font: ReaderFont) : ReaderLayoutChange
  data class FontScale(val scale: Int) : ReaderLayoutChange
  data class LineHeight(val lineHeight: Float) : ReaderLayoutChange
  data class Margin(val margin: ReaderMargin) : ReaderLayoutChange
  data class Justify(val justify: Boolean) : ReaderLayoutChange
  data class Hyphenate(val hyphenate: Boolean) : ReaderLayoutChange
  data class Flow(val flow: ReaderFlow) : ReaderLayoutChange
  data class Pages(val pages: ReaderPages) : ReaderLayoutChange

  fun applyTo(layout: ReaderLayout): ReaderLayout = when (this) {
    is Font -> layout.copy(font = font)
    is FontScale -> layout.copy(fontScale = scale.coerceIn(ReaderLayout.FontScaleRange))
    is LineHeight -> layout.copy(lineHeight = lineHeight.coerceIn(ReaderLayout.LineHeightRange))
    is Margin -> layout.copy(margin = margin)
    is Justify -> layout.copy(justify = justify)
    is Hyphenate -> layout.copy(hyphenate = hyphenate)
    is Flow -> layout.copy(flow = flow)
    is Pages -> layout.copy(pages = pages)
  }

  fun applyTo(overrides: ReaderLayoutOverrides): ReaderLayoutOverrides = when (this) {
    is Font -> overrides.copy(font = font)
    is FontScale -> overrides.copy(fontScale = scale.coerceIn(ReaderLayout.FontScaleRange))
    is LineHeight -> overrides.copy(lineHeight = lineHeight.coerceIn(ReaderLayout.LineHeightRange))
    is Margin -> overrides.copy(margin = margin)
    is Justify -> overrides.copy(justify = justify)
    is Hyphenate -> overrides.copy(hyphenate = hyphenate)
    is Flow -> overrides.copy(flow = flow)
    is Pages -> overrides.copy(pages = pages)
  }
}

internal fun Color.toCss(): String = "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0')
