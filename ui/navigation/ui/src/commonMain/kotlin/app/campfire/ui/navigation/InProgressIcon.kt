// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector

private const val SweepDurationMillis = 1_400
private const val FadeDurationMillis = 400

/** How tall the highlight band is, as a fraction of the icon's height. */
private const val SweepBandFraction = 0.9f

/**
 * An icon that, while [inProgress], has a band of [highlight] pour down through it over and over,
 * for work going on behind it such as downloads. The band fades in and out as the work starts and
 * stops, and nothing animates while it's idle.
 */
@Composable
internal fun InProgressIcon(
  imageVector: ImageVector,
  contentDescription: String?,
  inProgress: Boolean,
  modifier: Modifier = Modifier,
  highlight: Color = MaterialTheme.colorScheme.primary,
) {
  val strength = animateFloatAsState(
    targetValue = if (inProgress) 1f else 0f,
    animationSpec = tween(FadeDurationMillis),
    label = "in_progress_strength",
  )
  // Keeps sweeping while the band fades out, without recomposing on every frame of the fade
  val sweeping by remember {
    derivedStateOf { strength.value > 0f }
  }

  val sweepModifier = if (inProgress || sweeping) {
    val transition = rememberInfiniteTransition(label = "in_progress")
    val offset = transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(SweepDurationMillis, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
      ),
      label = "in_progress_offset",
    )
    Modifier.sweep(
      color = highlight,
      offset = { offset.value },
      strength = { strength.value },
    )
  } else {
    Modifier
  }

  Icon(
    imageVector = imageVector,
    contentDescription = contentDescription,
    modifier = modifier.then(sweepModifier),
  )
}

/**
 * Draws a band of [color] over the content's own pixels only, [offset] (0..1) of the way from just
 * above the content to just below it. Both are read while drawing, so the sweep only redraws.
 */
private fun Modifier.sweep(
  color: Color,
  offset: () -> Float,
  strength: () -> Float,
): Modifier = this
  // Offscreen so SrcAtop blends against the icon alone, not whatever is behind it
  .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
  .drawWithContent {
    drawContent()
    val band = size.height * SweepBandFraction
    val top = -band + (size.height + band) * offset()
    drawRect(
      brush = Brush.verticalGradient(
        0f to Color.Transparent,
        0.35f to color,
        0.65f to color,
        1f to Color.Transparent,
        startY = top,
        endY = top + band,
      ),
      alpha = strength(),
      blendMode = BlendMode.SrcAtop,
    )
  }
