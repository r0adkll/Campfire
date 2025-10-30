package app.campfire.audioplayer.ui.cast

import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Cast
import app.campfire.common.compose.icons.rounded.CastConnected
import app.campfire.common.compose.icons.rounded.CastConnecting

enum class CastButtonState {
  Unavailable,
  Searching,
  Disconnected,
  Connecting,
  Connected,
}

@Composable
fun CastButton(
  modifier: Modifier = Modifier,
  state: CastButtonState = CastButtonState.Unavailable,
) {
  if (state == CastButtonState.Unavailable) return

  IconButton(
    onClick = {},
    modifier = modifier,
  ) {
    when (state) {
      CastButtonState.Unavailable -> error("Invalid state for cast button")
      CastButtonState.Disconnected -> rememberVectorPainter(CampfireIcons.Rounded.Cast)

      CastButtonState.Searching,
      CastButtonState.Connecting -> CampfireIcons.Rounded.CastConnecting

      CastButtonState.Connected -> rememberVectorPainter(CampfireIcons.Rounded.CastConnected)
    }
  }
}



@Composable
expect fun PlatformCastButton(modifier: Modifier)
