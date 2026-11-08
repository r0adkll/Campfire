// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

val DefaultUpdateIndicatorSize = 12.dp

/**
 * A small badge announcing an app update: a rounded download arrow inside a filled circle. Every
 * few seconds the arrow drops out through the bottom of the circle and springs back in from the
 * top, like a file landing.
 *
 * Drawn rather than built from an icon so the arrow keeps a legible stroke at badge size.
 */
@Composable
fun UpdateIndicator(
  modifier: Modifier = Modifier,
  size: Dp = DefaultUpdateIndicatorSize,
  borderWidth: Dp = 2.dp,
  containerColor: Color = MaterialTheme.colorScheme.primary,
  arrowColor: Color = MaterialTheme.colorScheme.onPrimary,
  borderColor: Color = MaterialTheme.colorScheme.surface,
) {
  // Fraction of the badge's diameter the arrow is shifted down by; only read while drawing so the
  // loop never recomposes
  val drop = remember { Animatable(0f) }
  LaunchedEffect(drop) {
    while (true) {
      delay(DropIntervalMillis)
      drop.animateTo(1f, tween(durationMillis = 320, easing = EaseInCubic))
      drop.snapTo(-1f)
      drop.animateTo(
        targetValue = 0f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      )
    }
  }

  // The draw scope has its own `size`; name the badge's diameter so the two can't be confused
  val badgeSize = size
  Spacer(
    modifier = modifier
      .size(badgeSize + (borderWidth * 2))
      .drawWithCache {
        val diameter = badgeSize.toPx()
        val center = this.size.center
        val clip = Path().apply { addOval(Rect(center = center, radius = diameter / 2f)) }
        val stroke = Stroke(width = diameter * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Where the wings would meet; the curve below rounds it off short of this point
        val tipY = center.y + diameter * 0.22f
        val wing = diameter * 0.18f
        val tipRounding = diameter * 0.045f
        val arrow = Path().apply {
          // The shaft stops inside the rounded tip so its cap doesn't poke through
          moveTo(center.x, center.y - diameter * 0.20f)
          lineTo(center.x, tipY - tipRounding)
          moveTo(center.x - wing, tipY - wing)
          lineTo(center.x - tipRounding, tipY - tipRounding)
          quadraticTo(center.x, tipY, center.x + tipRounding, tipY - tipRounding)
          lineTo(center.x + wing, tipY - wing)
        }
        onDrawBehind {
          drawCircle(color = borderColor)
          drawCircle(color = containerColor, radius = diameter / 2f)
          clipPath(clip) {
            translate(top = drop.value * diameter) {
              drawPath(path = arrow, color = arrowColor, style = stroke)
            }
          }
        }
      },
  )
}

private const val DropIntervalMillis = 2_800L
