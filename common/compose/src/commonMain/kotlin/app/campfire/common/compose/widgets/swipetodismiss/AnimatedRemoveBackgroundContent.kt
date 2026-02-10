package app.campfire.common.compose.widgets.swipetodismiss

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import app.campfire.common.compose.icons.rounded.rememberMovingDeletePainter
import app.campfire.core.animations.lerp
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.action_remove
import org.jetbrains.compose.resources.stringResource

@Composable
fun RowScope.AnimatedRemoveBackgroundContent(
  state: SwipeToDismissBoxState,
  modifier: Modifier = Modifier,
) {
  val actualProgress = state
    .progress(SwipeToDismissBoxValue.Settled, SwipeToDismissBoxValue.EndToStart)
    .times(2f)
    .coerceIn(0f, 1f)
  val inverseEasedProgress = EaseInCubic.transform(actualProgress)

  val rotation = lerp(0f, 20f, inverseEasedProgress)
  val offset = androidx.compose.ui.unit.lerp(0.dp, 8.dp, inverseEasedProgress)
  var scale by remember { mutableFloatStateOf(1f) }

  val hapticFeedback = LocalHapticFeedback.current
  LaunchedEffect(state.targetValue) {
    if (state.targetValue == SwipeToDismissBoxValue.EndToStart) {
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
    modifier = Modifier
      .align(Alignment.CenterVertically)
      .padding(
        end = 4.dp,
      )
      .scale(scale)
      .offset {
        IntOffset(-offset.roundToPx(), 0)
      },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(Res.string.action_remove),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.error,
      fontWeight = FontWeight.Bold,
      modifier = Modifier
        .alpha(inverseEasedProgress),
    )

    Icon(
      rememberMovingDeletePainter(rotation),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.error,
      modifier = modifier.size(56.dp),
    )
  }
}
