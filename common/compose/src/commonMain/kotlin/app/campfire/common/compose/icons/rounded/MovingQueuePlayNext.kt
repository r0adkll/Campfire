// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons.rounded

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The rounded QueuePlayNext icon with moving parts, for a swipe that adds to the queue or takes out
 * of it. Only the plus moves: as [progress] goes from 0 to 1, adding grows it, while [removing]
 * folds it into a minus. Like [rememberMovingDeletePainter], the 24dp glyph sits in the middle of
 * a 56dp canvas.
 */
@Composable
fun rememberMovingQueuePlayNextPainter(
  progress: Float = 0f,
  removing: Boolean = false,
) = rememberVectorPainter(
  defaultWidth = 56.dp,
  defaultHeight = 56.dp,
  viewportWidth = 56f,
  viewportHeight = 56f,
  autoMirror = true,
  name = "MovingQueuePlayNext",
) { _, _ ->
  val cappedProgress = progress.coerceIn(0f, 1f)

  // The glyph is drawn on a 960 unit grid, scaled into the 24 units in the middle of the canvas
  Group(
    name = "Glyph",
    translationX = 16f,
    translationY = 16f,
    scaleX = GlyphScale,
    scaleY = GlyphScale,
  ) {
    Group(name = "Arrow") {
      Path(fill = SolidColor(Color.Black)) {
        moveToRelative(840f, 720f)
        lineToRelative(-90f, -90f)
        quadToRelative(-13f, -13f, -13f, -30f)
        reflectiveQuadToRelative(13f, -30f)
        quadToRelative(13f, -13f, 30f, -13f)
        reflectiveQuadToRelative(30f, 13f)
        lineToRelative(122f, 122f)
        quadToRelative(12f, 12f, 12f, 28f)
        reflectiveQuadToRelative(-12f, 28f)
        lineTo(810f, 870f)
        quadToRelative(-13f, 13f, -30f, 13f)
        reflectiveQuadToRelative(-30f, -13f)
        quadToRelative(-13f, -13f, -13f, -30f)
        reflectiveQuadToRelative(13f, -30f)
        lineToRelative(90f, -90f)
        close()
      }
    }

    Path(fill = SolidColor(Color.Black)) {
      moveTo(360f, 840f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(320f, 800f)
      verticalLineToRelative(-40f)
      lineTo(160f, 760f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(80f, 680f)
      verticalLineToRelative(-480f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(160f, 120f)
      horizontalLineToRelative(640f)
      quadToRelative(33f, 0f, 56.5f, 23.5f)
      reflectiveQuadTo(880f, 200f)
      verticalLineToRelative(240f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(840f, 480f)
      quadToRelative(-17f, 0f, -28.5f, -11.5f)
      reflectiveQuadTo(800f, 440f)
      verticalLineToRelative(-240f)
      lineTo(160f, 200f)
      verticalLineToRelative(480f)
      horizontalLineToRelative(480f)
      quadToRelative(17f, 0f, 28.5f, 11.5f)
      reflectiveQuadTo(680f, 720f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(640f, 760f)
      horizontalLineToRelative(-40f)
      verticalLineToRelative(40f)
      quadToRelative(0f, 17f, -11.5f, 28.5f)
      reflectiveQuadTo(560f, 840f)
      lineTo(360f, 840f)
      close()
    }

    Group(
      name = "Plus",
      scaleX = if (removing) 1f else 1f + PlusGrowth * cappedProgress,
      scaleY = if (removing) 1f else 1f + PlusGrowth * cappedProgress,
      pivotX = 480f,
      pivotY = 440f,
    ) {
      Path(name = "Bar", fill = SolidColor(Color.Black)) {
        moveTo(360f, 400f)
        lineTo(600f, 400f)
        arcTo(40f, 40f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 600f, y1 = 480f)
        lineTo(360f, 480f)
        arcTo(40f, 40f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 360f, y1 = 400f)
        close()
      }
      // The upright of the plus, which folds away to leave a minus when removing
      Group(
        name = "Upright",
        scaleY = if (removing) 1f - cappedProgress else 1f,
        pivotX = 480f,
        pivotY = 440f,
      ) {
        Path(fill = SolidColor(Color.Black)) {
          moveTo(440f, 320f)
          arcTo(40f, 40f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 520f, y1 = 320f)
          lineTo(520f, 560f)
          arcTo(40f, 40f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 440f, y1 = 560f)
          close()
        }
      }
    }
  }
}

private const val GlyphScale = 24f / 960f

/** How much bigger the plus grows when adding. */
private const val PlusGrowth = 0.3f

@Preview
@Composable
fun MovingQueuePlayNextPreview() {
  Box(modifier = Modifier.padding(12.dp)) {
    Image(
      rememberMovingQueuePlayNextPainter(progress = 0f),
      contentDescription = null,
      modifier = Modifier.size(56.dp),
    )
  }
}
