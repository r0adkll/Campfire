// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.swipeaction

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import app.campfire.common.compose.icons.rounded.rememberMovingDeletePainter
import app.campfire.core.animations.lerp
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.swipe_delete
import org.jetbrains.compose.resources.stringResource

/**
 * The delete action uncovered at the end of a [SwipeActionBox] as it is swiped toward the start,
 * the counterpart of [app.campfire.common.compose.widgets.swipetodismiss.AnimatedRemoveBackgroundContent]
 * for a row that springs back: the bin's lid tips open and the label fades in as the swipe nears
 * the threshold, and both pop when it's reached.
 */
@Composable
fun BoxScope.AnimatedDeleteBackgroundContent(
  state: SwipeActionState,
  modifier: Modifier = Modifier,
) {
  val easedProgress = EaseInCubic.transform(state.progress)
  val rotation = lerp(0f, 20f, easedProgress)
  val offset = lerp(0.dp, 8.dp, easedProgress)
  var scale by remember { mutableFloatStateOf(1f) }

  val isPastThreshold = state.isPastThreshold
  val hapticFeedback = LocalHapticFeedback.current
  LaunchedEffect(isPastThreshold) {
    if (isPastThreshold) {
      hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)

      animate(1f, 1.2f) { value, _ -> scale = value }
      animate(
        initialValue = 1.2f,
        targetValue = 1f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow,
        ),
      ) { value, _ ->
        scale = value
      }
    }
  }

  Row(
    modifier = modifier
      .align(Alignment.CenterEnd)
      .padding(end = 4.dp)
      .scale(scale)
      .offset {
        IntOffset(-offset.roundToPx(), 0)
      },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(Res.string.swipe_delete),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.error,
      fontWeight = FontWeight.Bold,
      modifier = Modifier
        .alpha(easedProgress),
    )

    Icon(
      rememberMovingDeletePainter(rotation),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.error,
      modifier = Modifier.size(56.dp),
    )
  }
}
