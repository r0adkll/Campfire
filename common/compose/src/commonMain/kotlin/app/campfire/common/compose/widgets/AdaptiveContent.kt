package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AdaptiveExpandedThreshold = 420.dp

enum class AdaptiveState {
  Compact,
  Expanded,
}

@Composable
fun AdaptiveContent(
  compact: @Composable () -> Unit,
  expanded: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  stateResolver: (maxWidth: Dp) -> AdaptiveState = { maxWidth ->
    if (maxWidth > AdaptiveExpandedThreshold) AdaptiveState.Expanded else AdaptiveState.Compact
  },
) {
  BoxWithConstraints(modifier = modifier) {
    when (stateResolver(maxWidth)) {
      AdaptiveState.Compact -> compact()
      AdaptiveState.Expanded -> expanded()
    }
  }
}
