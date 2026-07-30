// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.plugin.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.android.plugin.common.LoadingIndicator
import app.campfire.android.plugin.common.LogRow
import app.campfire.android.plugin.common.SectionButton
import app.campfire.android.plugin.common.SectionHeader
import com.livewire.ui.actions.clickAction
import com.livewire.ui.graphics.RoundedCornerShape
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
import com.livewire.ui.widget.ResizableSurface
import com.livewire.ui.widget.ResizeAnchor
import com.livewire.ui.widget.Spacer
import com.livewire.ui.widget.Surface
import com.livewire.ui.widget.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Split-pane session inspector: connected controllers and the session callback log on
 * the left, and the live ExoPlayer AnalyticsListener log in the side panel.
 */
@Composable
internal fun ControllersTab() {
  val session by SessionDebugCollector.session.collectAsState()
  val events by SessionDebugCollector.events.collectAsState()

  Row(LivewireModifier.fillMaxSize()) {
    // Left pane: connected controllers + session callback log
    Column(
      LivewireModifier
        .weight(1f)
        .fillMaxHeight()
        .verticalScroll()
        .padding(
          horizontal = 16.dp,
        ),
    ) {
      SectionHeader("Connected controllers")
      val currentSession = session
      if (currentSession == null) {
        Text(
          text = "No MediaLibrarySession — AudioPlayerService is not running.",
          color = Color.Gray,
        )
      } else {
        var controllers by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
        LaunchedEffect(currentSession) {
          while (true) {
            controllers = withContext(Dispatchers.Main) {
              currentSession.connectedControllers.map { controller ->
                controller.packageName to SessionDebugCollector.describe(currentSession, controller)
              }
            }
            delay(1_000L)
          }
        }
        val currentControllers = controllers
        when {
          currentControllers == null -> LoadingIndicator(
            message = "Querying connected controllers…",
            modifier = LivewireModifier.fillMaxWidth().padding(16.dp),
          )
          currentControllers.isEmpty() -> Text("No controllers connected.", color = Color.Gray)
          else -> currentControllers.forEach { (packageName, info) ->
            ControllerCard(packageName, info)
          }
        }
      }

      Spacer(LivewireModifier.height(16.dp))
      SectionHeader("Session callback log") {
        SectionButton(
          action = clickAction { SessionDebugCollector.clear() },
          modifier = LivewireModifier.padding(2.dp),
        ) { Text("Clear") }
      }
      if (events.isEmpty()) {
        Spacer(LivewireModifier.height(8.dp))
        Text(
          text = "No events yet — connect a controller or send a command.",
          color = Color.Gray,
        )
      } else {
        val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        events.forEach { event ->
          LogRow(
            time = timeFormat.format(Date(event.timeMs)),
            type = event.type,
            typeColor = event.type.asSessionTypeColor(),
            source = event.packageName.takeIf { it != "-" },
            details = event.details,
          )
        }
      }
    }

    // Side panel: live AnalyticsListener event log
    ResizableSurface(
      anchor = ResizeAnchor.Start,
      initialSize = 420.dp,
      minSize = 300.dp,
      tonalElevation = 2.dp,
    ) {
      AnalyticsLogPanel()
    }
  }
}

@Composable
private fun ControllerCard(packageName: String, info: String) {
  Surface(
    modifier = LivewireModifier
      .fillMaxWidth()
      .padding(
        vertical = 2.dp,
      ),
    shape = RoundedCornerShape(8.dp),
    tonalElevation = 1.dp,
  ) {
    Column(
      LivewireModifier
        .fillMaxWidth()
        .padding(8.dp),
    ) {
      Text(
        text = packageName,
        style = LivewireTheme.typography.titleSmall,
      )
      Text(
        text = info,
        style = LivewireTheme.typography.bodySmall,
        color = Color.Gray,
      )
    }
  }
}

@Composable
private fun AnalyticsLogPanel() {
  val analyticsEvents by SessionDebugCollector.analyticsEvents.collectAsState()

  Column(
    modifier = LivewireModifier.fillMaxSize(),
  ) {
    Surface(
      color = LivewireTheme.colorScheme.surfaceContainerHigh,
    ) {
      Row(
        modifier = LivewireModifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(
            horizontal = 16.dp,
          ),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "ExoPlayer analytics",
          modifier = LivewireModifier.weight(1f),
          style = LivewireTheme.typography.titleMedium,
        )
        SectionButton(
          action = clickAction { SessionDebugCollector.clearAnalytics() },
        ) { Text("Clear") }
      }
    }

    if (analyticsEvents.isEmpty()) {
      Text(
        text = "No analytics events yet — start playback to see decoder, format, and state events.",
        color = Color.Gray,
        style = LivewireTheme.typography.bodySmall,
      )
    } else {
      val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
      Column(
        LivewireModifier
          .fillMaxSize()
          .padding(horizontal = 16.dp)
          .verticalScroll(),
      ) {
        analyticsEvents.forEach { event ->
          LogRow(
            time = timeFormat.format(Date(event.timeMs)),
            type = event.type,
            typeColor = event.type.asAnalyticsTypeColor(),
            source = null,
            details = event.details,
          )
        }
      }
    }
  }
}

private fun String.asSessionTypeColor(): Color = when (this) {
  "connect" -> Color.Green
  "customCommand" -> Color.Cyan
  "mediaButton" -> Color.Yellow
  "session" -> Color.Magenta
  else -> Color.LightGray
}

private fun String.asAnalyticsTypeColor(): Color = when (this) {
  "playerError", "loadError" -> Color.Red
  "audioUnderrun" -> Color.Yellow
  "stateChanged", "isPlaying" -> Color.Cyan
  "mediaItemTransition", "positionDiscontinuity" -> Color.Magenta
  else -> Color.LightGray
}
