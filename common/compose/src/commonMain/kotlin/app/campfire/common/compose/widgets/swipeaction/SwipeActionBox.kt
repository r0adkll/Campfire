// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.swipeaction

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Where a [SwipeActionBox]'s content sits while it is swiped, and how close the swipe is to firing
 * its action.
 */
@Stable
class SwipeActionState internal constructor() {

  internal var offset by mutableFloatStateOf(0f)

  internal var threshold by mutableFloatStateOf(Float.POSITIVE_INFINITY)

  /** How far the content has been swiped, as a fraction of the distance that fires the action. */
  val progress: Float
    get() = (offset / threshold).coerceIn(0f, 1f)

  /** Whether letting go now fires the action. */
  val isPastThreshold: Boolean
    get() = offset >= threshold
}

@Composable
fun rememberSwipeActionState(): SwipeActionState = remember { SwipeActionState() }

/**
 * Content that can be swiped in the reading direction to fire [onSwipe]. Unlike a swipe to
 * dismiss, the content springs back once let go, whether or not the action fired, so it suits
 * actions that leave the row in place (adding to a queue, say).
 *
 * The action fires when the content is let go past [threshold], or past 45% of its width if that
 * is closer. Past that point the content keeps following the finger, but with resistance.
 *
 * @param onSwipe called when a swipe is let go past the threshold.
 * @param backgroundContent drawn behind [content] and uncovered as it is swiped. Read the
 *   [SwipeActionState] to animate it with the swipe, and to give feedback, such as a haptic, as
 *   it crosses the threshold.
 * @param enabled whether the content can be swiped.
 * @param threshold how far the content must be swiped to fire [onSwipe].
 */
@Composable
fun SwipeActionBox(
  onSwipe: () -> Unit,
  backgroundContent: @Composable BoxScope.(SwipeActionState) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  state: SwipeActionState = rememberSwipeActionState(),
  threshold: Dp = SwipeActionDefaults.Threshold,
  content: @Composable () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val currentOnSwipe by rememberUpdatedState(onSwipe)
  val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
  val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

  // Where the finger is, which runs ahead of the content once past the threshold
  var dragDistance by remember { mutableFloatStateOf(0f) }
  var settling by remember { mutableStateOf<Job?>(null) }

  val draggableState = rememberDraggableState { delta ->
    dragDistance = (dragDistance + delta).coerceAtLeast(0f)
    val isPastThreshold = dragDistance >= state.threshold
    val resisted = if (isPastThreshold) {
      state.threshold + (dragDistance - state.threshold) * OvershootResistance
    } else {
      dragDistance
    }
    state.offset = resisted
  }

  Box(
    modifier = modifier
      .onSizeChanged { size ->
        state.threshold = minOf(thresholdPx, size.width * MaxThresholdFraction)
      }
      .draggable(
        state = draggableState,
        orientation = Orientation.Horizontal,
        enabled = enabled,
        // Swiping toward the end of the line is the reading direction, which is leftward in RTL
        reverseDirection = isRtl,
        // Picks up from wherever the content is, should it be caught while springing back
        onDragStarted = {
          settling?.cancel()
          dragDistance = state.offset.coerceAtLeast(0f)
        },
        onDragStopped = {
          if (dragDistance >= state.threshold) currentOnSwipe()
          dragDistance = 0f
          settling = scope.launch {
            animate(
              initialValue = state.offset,
              targetValue = 0f,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
              ),
            ) { value, _ -> state.offset = value }
          }
        },
      ),
    propagateMinConstraints = true,
  ) {
    Box(Modifier.matchParentSize()) {
      backgroundContent(state)
    }
    Box(
      modifier = Modifier.offset {
        // offset is mirrored in RTL, so a positive offset moves toward the end of the line
        IntOffset(state.offset.roundToInt(), 0)
      },
      propagateMinConstraints = true,
    ) {
      content()
    }
  }
}

object SwipeActionDefaults {
  /** The swipe distance that fires the action, unless the content is too narrow for it. */
  val Threshold: Dp = 144.dp
}

/** How much of the drag past the threshold the content follows. */
private const val OvershootResistance = 0.3f

/** The furthest the threshold can be, as a share of the content's width, for narrow content. */
private const val MaxThresholdFraction = 0.45f
