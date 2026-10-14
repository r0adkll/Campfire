// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Check

/**
 * Dims the camera's view around a square guide with corner brackets, showing where to hold the
 * code. Any code in view is read, so the guide is only a suggestion. Once [captured], the guide
 * takes a tint of the theme's primary color and a check appears inside. [hint] sits just below the
 * guide, and [controls] below that.
 */
@Composable
internal fun ViewfinderGuide(
  captured: Boolean,
  hint: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  controls: @Composable () -> Unit = {},
) {
  val tint by animateFloatAsState(if (captured) CapturedTintAlpha else 0f)
  val tintColor = MaterialTheme.colorScheme.primary

  BoxWithConstraints(modifier) {
    val guideSize = (min(maxWidth, maxHeight) * GuideFraction).coerceIn(MinGuideSize, MaxGuideSize)
    // Sits a little above center, leaving room below for the hint and controls
    val guideTop = (maxHeight - guideSize) * GuideVerticalBias

    Canvas(
      Modifier
        .fillMaxSize()
        // Offscreen so the cleared guide shows the camera through the scrim
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
      val guide = Rect(
        offset = Offset((size.width - guideSize.toPx()) / 2f, guideTop.toPx()),
        size = Size(guideSize.toPx(), guideSize.toPx()),
      )
      val radius = CornerRadius(GuideCornerRadius.toPx())

      drawRect(ScrimColor)
      drawRoundRect(
        color = Color.Black,
        topLeft = guide.topLeft,
        size = guide.size,
        cornerRadius = radius,
        blendMode = BlendMode.Clear,
      )
      if (tint > 0f) {
        drawRoundRect(
          color = tintColor.copy(alpha = tint),
          topLeft = guide.topLeft,
          size = guide.size,
          cornerRadius = radius,
        )
      }
      drawCornerBrackets(guide, Color.White)
    }

    AnimatedVisibility(
      visible = captured,
      enter = scaleIn() + fadeIn(),
      exit = scaleOut() + fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .offset(y = guideTop + (guideSize - CheckSize) / 2),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(CheckSize),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(CampfireIcons.Rounded.Check, contentDescription = null, modifier = Modifier.size(40.dp))
        }
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = guideTop + guideSize + 24.dp)
        .padding(horizontal = 24.dp),
    ) {
      hint()
      controls()
    }
  }
}

private fun DrawScope.drawCornerBrackets(guide: Rect, color: Color) {
  // Drawn along the cutout's own edge, so the brackets follow its rounded corners
  val radius = GuideCornerRadius.toPx()
  val arm = BracketArm.toPx()
  val diameter = Size(radius * 2, radius * 2)
  val path = Path().apply {
    // Each corner: an arm along one edge, around the corner, an arm along the other
    fun corner(arc: Rect, startAngle: Float, start: Offset, end: Offset) {
      moveTo(start.x, start.y)
      arcTo(arc, startAngle, 90f, forceMoveTo = false)
      lineTo(end.x, end.y)
    }
    corner(
      arc = Rect(guide.topLeft, diameter),
      startAngle = 180f,
      start = Offset(guide.left, guide.top + radius + arm),
      end = Offset(guide.left + radius + arm, guide.top),
    )
    corner(
      arc = Rect(Offset(guide.right - radius * 2, guide.top), diameter),
      startAngle = 270f,
      start = Offset(guide.right - radius - arm, guide.top),
      end = Offset(guide.right, guide.top + radius + arm),
    )
    corner(
      arc = Rect(Offset(guide.right - radius * 2, guide.bottom - radius * 2), diameter),
      startAngle = 0f,
      start = Offset(guide.right, guide.bottom - radius - arm),
      end = Offset(guide.right - radius - arm, guide.bottom),
    )
    corner(
      arc = Rect(Offset(guide.left, guide.bottom - radius * 2), diameter),
      startAngle = 90f,
      start = Offset(guide.left + radius + arm, guide.bottom),
      end = Offset(guide.left, guide.bottom - radius - arm),
    )
  }
  drawPath(path, color, style = Stroke(width = BracketStroke.toPx(), cap = StrokeCap.Round))
}

private const val GuideFraction = 0.68f
private const val GuideVerticalBias = 0.4f
private const val CapturedTintAlpha = 0.2f
private val ScrimColor = Color.Black.copy(alpha = 0.55f)
private val MinGuideSize: Dp = 200.dp
private val MaxGuideSize: Dp = 320.dp
private val GuideCornerRadius: Dp = 28.dp
private val BracketArm: Dp = 20.dp
private val BracketStroke: Dp = 5.dp
private val CheckSize: Dp = 72.dp
