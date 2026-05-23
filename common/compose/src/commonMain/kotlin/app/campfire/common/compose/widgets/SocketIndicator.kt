package app.campfire.common.compose.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme

enum class ConnectionState {
  Connecting,
  Connected,
  Disconnected,
}

val DefaultConnectionIndicatorSize = 8.dp

@Composable
fun ConnectionIndicator(
  state: ConnectionState,
  modifier: Modifier = Modifier,
  size: Dp = DefaultConnectionIndicatorSize,
  borderWidth: Dp = 2.dp,
  borderColor: Color = MaterialTheme.colorScheme.surface,
) {
  val indicatorColor by animateColorAsState(
    when (state) {
      ConnectionState.Connected -> CampfireTheme.colorScheme.success
      ConnectionState.Connecting -> CampfireTheme.colorScheme.loading
      ConnectionState.Disconnected -> MaterialTheme.colorScheme.error
    },
  )

  val infiniteTransition = rememberInfiniteTransition("socket_indicator_alpha")

  val indicatorAlpha = when (state) {
    ConnectionState.Connected,
    ConnectionState.Disconnected,
    -> 1f

    ConnectionState.Connecting -> infiniteTransition.animateFloat(
      0.3f,
      0.8f,
      animationSpec = infiniteRepeatable(
        tween(500, easing = EaseInOutCubic),
        RepeatMode.Reverse,
      ),
    ).value
  }

  Canvas(
    modifier = modifier
      .size(size + (borderWidth * 2)),
  ) {
    drawCircle(
      color = borderColor,
    )
    drawCircle(
      color = indicatorColor,
      alpha = indicatorAlpha,
      radius = size.toPx() / 2.0f,
    )
  }
}
