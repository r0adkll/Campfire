package app.campfire.android.plugin.campfire

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.android.plugin.common.LogRow
import app.campfire.android.plugin.common.SectionButton
import app.campfire.android.plugin.common.SectionHeader
import app.campfire.socket.SocketManager
import app.campfire.socket.SocketState
import com.livewire.ui.actions.clickAction
import com.livewire.ui.layout.Alignment
import com.livewire.ui.layout.Column
import com.livewire.ui.layout.Row
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.fillMaxHeight
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.height
import com.livewire.ui.modifier.padding
import com.livewire.ui.modifier.verticalScroll
import com.livewire.ui.theme.LivewireTheme
import com.livewire.ui.widget.CodeBlock
import com.livewire.ui.widget.ResizableSurface
import com.livewire.ui.widget.ResizeAnchor
import com.livewire.ui.widget.Spacer
import com.livewire.ui.widget.Surface
import com.livewire.ui.widget.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Split-pane socket inspector: live connection state, transition history, and retry
 * on the left; the socket event log (with per-event payload detail) in the side panel.
 */
@Composable
internal fun SocketTab(socketManager: SocketManager) {
  val state by socketManager.state.collectAsState()
  val history by SocketDebugCollector.stateHistory.collectAsState()

  Row(LivewireModifier.fillMaxSize()) {
    // Left pane: connection state + transition history
    Column(
      LivewireModifier
        .weight(1f)
        .fillMaxHeight()
        .verticalScroll()
        .padding(horizontal = 16.dp),
    ) {
      SectionHeader("Connection") {
        SectionButton(
          action = clickAction { socketManager.retryConnection() },
        ) { Text("Retry connection") }
      }
      Surface(
        modifier = LivewireModifier.fillMaxWidth(),
        tonalElevation = 3.dp,
      ) {
        Column(
          LivewireModifier
            .fillMaxWidth()
            .padding(12.dp),
        ) {
          Text(
            text = state.displayName(),
            style = LivewireTheme.typography.titleMedium,
            color = state.displayColor(),
          )
          Text(
            text = state.displayDetails(),
            style = LivewireTheme.typography.bodySmall,
            color = Color.Gray,
          )
        }
      }

      SectionHeader("State history")
      if (history.isEmpty()) {
        Text("No transitions recorded yet.", color = Color.Gray)
      } else {
        val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        history.forEach { transition ->
          LogRow(
            time = timeFormat.format(Date(transition.timeMs)),
            type = transition.state.displayName(),
            typeColor = transition.state.displayColor(),
            source = null,
            details = transition.state.displayDetails(),
          )
        }
      }
    }

    // Side panel: socket event log
    ResizableSurface(
      anchor = ResizeAnchor.Start,
      initialSize = 420.dp,
      minSize = 300.dp,
      tonalElevation = 2.dp,
    ) {
      SocketEventPanel()
    }
  }
}

@Composable
private fun SocketEventPanel() {
  val events by SocketDebugCollector.events.collectAsState()
  var selectedId by remember { mutableStateOf<Long?>(null) }
  val selected = events.find { it.id == selectedId }

  Column(LivewireModifier.fillMaxSize()) {
    Surface(
      color = LivewireTheme.colorScheme.surfaceContainerHigh,
    ) {
      Row(
        modifier = LivewireModifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (selected != null) selected.name else "Socket events",
          modifier = LivewireModifier.weight(1f),
          style = LivewireTheme.typography.titleMedium,
        )
        if (selected != null) {
          SectionButton(action = clickAction { selectedId = null }) { Text("Back") }
        } else {
          SectionButton(action = clickAction { SocketDebugCollector.clearEvents() }) { Text("Clear") }
        }
      }
    }

    if (selected != null) {
      // Payload detail for the selected event
      CodeBlock(
        content = selected.details,
        modifier = LivewireModifier
          .fillMaxWidth()
          .weight(1f)
          .padding(16.dp),
      )
    } else if (events.isEmpty()) {
      Text(
        text = "No socket events yet — they appear as the server pushes updates.",
        color = Color.Gray,
        style = LivewireTheme.typography.bodySmall,
        modifier = LivewireModifier.padding(16.dp),
      )
    } else {
      val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
      Column(
        LivewireModifier
          .fillMaxSize()
          .verticalScroll()
          .padding(horizontal = 16.dp),
      ) {
        Spacer(LivewireModifier.height(4.dp))
        events.forEach { event ->
          Surface(
            modifier = LivewireModifier
              .fillMaxWidth()
              .padding(vertical = 2.dp),
            tonalElevation = 1.dp,
            onClick = clickAction(key = "socket_event_${event.id}") { selectedId = event.id },
          ) {
            Row(
              LivewireModifier
                .fillMaxWidth()
                .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = event.name,
                modifier = LivewireModifier.weight(1f),
                style = LivewireTheme.typography.titleSmall,
              )
              Text(
                text = timeFormat.format(Date(event.timeMs)),
                style = LivewireTheme.typography.labelSmall,
                color = Color.Gray,
              )
            }
          }
        }
      }
    }
  }
}

internal fun SocketState.displayName(): String = when (this) {
  is SocketState.Authenticated -> "Authenticated"
  SocketState.Authenticating -> "Authenticating"
  SocketState.Connecting -> "Connecting"
  SocketState.Disabled -> "Disabled"
  SocketState.Disconnected -> "Disconnected"
  is SocketState.Failed -> "Failed"
}

internal fun SocketState.displayDetails(): String = when (this) {
  is SocketState.Authenticated -> "userId=$userId, username=$username"
  is SocketState.Failed -> "reason=$reason"
  else -> "—"
}

internal fun SocketState.displayColor(): Color = when (this) {
  is SocketState.Authenticated -> Color.Green
  SocketState.Authenticating -> Color.Cyan
  SocketState.Connecting -> Color.Yellow
  SocketState.Disabled -> Color.LightGray
  SocketState.Disconnected -> Color.Gray
  is SocketState.Failed -> Color.Red
}
