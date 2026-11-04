// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.layout

import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * How a theme screen arranges its preview and the rest (the builder's controls, the picker's
 * themes) in the space it has.
 */
@Immutable
internal sealed interface PreviewLayout {

  /** Phones: the screen's compact layout, such as the builder's tray or the picker's gallery. */
  data object Compact : PreviewLayout

  /**
   * A foldable half-open on a table: the preview above the hinge, the rest below it, filling the
   * lower half.
   */
  data class Tabletop(val topHeight: Dp, val hingeHeight: Dp) : PreviewLayout

  /**
   * Wider windows: a column beside the preview, [controlsWidth] wide and [gap] from it. With
   * [dual], light and dark previews show side by side.
   */
  data class Side(val controlsWidth: Dp, val gap: Dp, val dual: Boolean) : PreviewLayout
}

/**
 * A fold that divides the screen, from [start] to [end] along the axis it crosses: top to
 * bottom for a horizontal fold, start to end for a vertical one.
 */
@Immutable
internal data class FoldHinge(val start: Dp, val end: Dp, val isVertical: Boolean)

/**
 * The fold dividing the screen's content, which sits at [content] in the window, or null if
 * none does. A horizontal fold only counts in tabletop posture, and only when the app hasn't
 * already docked the player below it ([playerDocked]), since the screen then has the top half
 * alone. A vertical fold counts when it separates the screen, as a half-open foldable held like a
 * book does.
 */
internal fun foldHinge(
  posture: Posture,
  content: Rect,
  density: Density,
  playerDocked: Boolean,
): FoldHinge? {
  if (content.isEmpty) return null
  val horizontal = posture.hingeList.firstOrNull { !it.isVertical }?.takeIf { posture.isTabletop && !playerDocked }
  val vertical = posture.hingeList.firstOrNull { it.isVertical && it.isSeparating }
  return with(density) {
    when {
      horizontal != null -> FoldHinge(
        start = (horizontal.bounds.top - content.top).toDp(),
        end = (horizontal.bounds.bottom - content.top).toDp(),
        isVertical = false,
      )
      vertical != null -> FoldHinge(
        start = (vertical.bounds.left - content.left).toDp(),
        end = (vertical.bounds.right - content.left).toDp(),
        isVertical = true,
      )
      else -> null
    }
  }
}

/**
 * The layout for a screen of [size], divided by [hinge] if there is one. A tabletop fold splits
 * it around the hinge, whatever the width. Otherwise phones get the compact layout, and wider
 * windows a column beside the preview: lined up with a vertical fold when there is one, and with
 * light and dark previews when both fit at [MinPreviewWidth].
 */
internal fun previewLayout(size: DpSize, hinge: FoldHinge?): PreviewLayout {
  if (hinge != null && !hinge.isVertical) {
    val bottom = size.height - hinge.end
    if (hinge.start >= MinTabletopRegion && bottom >= MinTabletopRegion) {
      return PreviewLayout.Tabletop(topHeight = hinge.start, hingeHeight = hinge.end - hinge.start)
    }
  }

  if (size.width < SideMinWidth) return PreviewLayout.Compact

  if (hinge != null && hinge.isVertical) {
    val previewWidth = size.width - hinge.end - SidePreviewEndPadding
    if (hinge.start in MinControlsWidth..MaxFoldControlsWidth && previewWidth >= MinPreviewWidth) {
      return PreviewLayout.Side(controlsWidth = hinge.start, gap = hinge.end - hinge.start, dual = false)
    }
  }

  val controlsWidth = (size.width * ControlsWidthFraction).coerceIn(MinControlsWidth, MaxControlsWidth)
  val previewWidth = size.width - controlsWidth - SideGap - SidePreviewEndPadding
  return PreviewLayout.Side(
    controlsWidth = controlsWidth,
    gap = SideGap,
    dual = previewWidth >= MinPreviewWidth * 2 + DualPreviewGap,
  )
}

/** Below this width a column and the preview can't sit side by side. */
internal val SideMinWidth = 600.dp

/** Less than this either side of a tabletop fold, and the window is better laid out as one. */
private val MinTabletopRegion = 200.dp

private const val ControlsWidthFraction = 0.45f
private val MinControlsWidth = 320.dp
private val MaxControlsWidth = 400.dp

/** A vertical fold can leave the column a little wider than usual, as its half of the screen. */
private val MaxFoldControlsWidth = 560.dp

/**
 * The narrowest a preview gets beside the column: a phone-sized scene still draws at about
 * 80%. Two of these fit beside the column from about 1000dp, a tablet held sideways.
 */
internal val MinPreviewWidth = 280.dp

internal val SideGap = 8.dp
internal val SidePreviewEndPadding = 24.dp
internal val DualPreviewGap = 16.dp
