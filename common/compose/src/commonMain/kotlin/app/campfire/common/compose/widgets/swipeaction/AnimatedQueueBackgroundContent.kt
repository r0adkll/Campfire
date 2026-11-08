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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import app.campfire.common.compose.icons.rounded.rememberMovingQueuePlayNextPainter
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.swipe_episode_dequeue
import campfire.common.compose.generated.resources.swipe_episode_queue
import org.jetbrains.compose.resources.stringResource

/**
 * The queue action uncovered behind a [SwipeActionBox] as it is swiped, the counterpart of
 * [app.campfire.common.compose.widgets.swipetodismiss.AnimatedRemoveBackgroundContent]: the icon
 * slides out and comes alive and the label fades in as the swipe nears the threshold, and both pop
 * when it's reached. An episode that [isQueued] offers to take it out of the queue instead.
 */
@Composable
fun BoxScope.AnimatedQueueBackgroundContent(
  state: SwipeActionState,
  isQueued: Boolean,
  modifier: Modifier = Modifier,
) {
  // What the swipe does stays put until the row is back at rest, so the row springing back after
  // queueing an episode doesn't flash the action that takes it out again
  var showsQueued by remember { mutableStateOf(isQueued) }
  val currentIsQueued by rememberUpdatedState(isQueued)
  LaunchedEffect(state) {
    snapshotFlow { state.progress == 0f && currentIsQueued != showsQueued }
      .collect { changed -> if (changed) showsQueued = currentIsQueued }
  }

  val easedProgress = EaseInCubic.transform(state.progress)
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

  val color = if (showsQueued) {
    MaterialTheme.colorScheme.error
  } else {
    MaterialTheme.colorScheme.primary
  }

  Row(
    modifier = modifier
      .align(Alignment.CenterStart)
      .padding(start = 4.dp)
      .scale(scale)
      .offset {
        IntOffset(offset.roundToPx(), 0)
      },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      rememberMovingQueuePlayNextPainter(progress = easedProgress, removing = showsQueued),
      contentDescription = null,
      tint = color,
      modifier = Modifier.size(56.dp),
    )

    Text(
      text = stringResource(
        if (showsQueued) Res.string.swipe_episode_dequeue else Res.string.swipe_episode_queue,
      ),
      style = MaterialTheme.typography.labelLarge,
      color = color,
      fontWeight = FontWeight.Bold,
      modifier = Modifier
        .alpha(easedProgress),
    )
  }
}
