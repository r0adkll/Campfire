package app.campfire.common.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.icon
import app.campfire.common.compose.icons.rememberTentVectorPainter
import app.campfire.common.compose.widgets.AppBarState.ConnectionState
import app.campfire.common.compose.widgets.AppBarState.ConnectionState.Connected
import app.campfire.common.compose.widgets.AppBarState.ConnectionState.Connecting
import app.campfire.common.compose.widgets.AppBarState.ConnectionState.Disconnected
import app.campfire.common.compose.widgets.AppBarState.ConnectionState.None
import app.campfire.core.model.Server
import app.campfire.core.model.Tent

val DefaultServerIconSize = 40.dp

sealed interface ServerState {
  data object Loading : ServerState
  data class Loaded(
    val server: Server,
    val useDynamicColors: Boolean,
    val connectionState: ConnectionState,
  ) : ServerState
  data object Error : ServerState
}

@Composable
fun ServerIcon(
  serverState: ServerState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = DefaultServerIconSize,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier
      .clip(CircleShape)
      .clickable(onClick = onClick),
  ) {
    when (serverState) {
      ServerState.Loading -> {
        Image(
          Tent.Default.icon,
          contentDescription = null,
          modifier = Modifier
            .size(size)
            .padding(4.dp),
        )
      }

      is ServerState.Loaded -> {
        if (serverState.useDynamicColors) {
          Image(
            rememberTentVectorPainter(),
            contentDescription = null,
            modifier = Modifier
              .size(size)
              .padding(4.dp),
          )
        } else {
          Image(
            serverState.server.tent.icon,
            contentDescription = null,
            modifier = Modifier
              .size(size)
              .padding(4.dp),
          )
        }
        if (serverState.connectionState != None) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(
                when (serverState.connectionState) {
                  Disconnected -> MaterialTheme.colorScheme.error
                  Connecting -> Color.Yellow
                  Connected -> Color.Green
                  None -> Color.Transparent
                },
                CircleShape,
              ),
          )
        }
      }

      ServerState.Error -> {
        Image(
          Tent.Default.icon,
          contentDescription = null,
          modifier = Modifier
            .size(size)
            .padding(4.dp),
        )
      }
    }
  }
}
