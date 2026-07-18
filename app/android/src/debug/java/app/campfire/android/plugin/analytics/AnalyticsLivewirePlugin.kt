package app.campfire.android.plugin.analytics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.android.plugin.analytics.LivewireAnalytics.RecordedEvent
import app.campfire.android.plugin.common.SectionButton
import app.campfire.android.plugin.common.SectionHeader
import app.campfire.android.plugin.common.SegmentedSection
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Analytics
import com.livewire.ui.Plugin
import com.livewire.ui.PluginInfo
import com.livewire.ui.actions.ClickAction
import com.livewire.ui.actions.clickAction
import com.livewire.ui.actions.valueChangeAction
import com.livewire.ui.composition.LivewireComposable
import com.livewire.ui.graphics.CircleShape
import com.livewire.ui.graphics.RoundedCornerShape
import com.livewire.ui.layout.Alignment
import com.livewire.ui.layout.Column
import com.livewire.ui.layout.ColumnScope
import com.livewire.ui.layout.Row
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.background
import com.livewire.ui.modifier.fillMaxHeight
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.height
import com.livewire.ui.modifier.padding
import com.livewire.ui.modifier.verticalScroll
import com.livewire.ui.modifier.width
import com.livewire.ui.theme.LivewireTheme
import com.livewire.ui.widget.AnimatedVisibility
import com.livewire.ui.widget.BasicTextField
import com.livewire.ui.widget.Button
import com.livewire.ui.widget.ButtonShapes
import com.livewire.ui.widget.ButtonSize
import com.livewire.ui.widget.ButtonStyle
import com.livewire.ui.widget.Icon
import com.livewire.ui.widget.ResizableSurface
import com.livewire.ui.widget.ResizeAnchor
import com.livewire.ui.widget.Spacer
import com.livewire.ui.widget.Surface
import com.livewire.ui.widget.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Inspects the events flowing through Campfire's analytics pipeline: a filterable
 * master list fed by [LivewireAnalytics], with a side panel detailing the selected
 * event's parameters.
 */
class AnalyticsLivewirePlugin : Plugin {

  override val info: PluginInfo = PluginInfo(
    pluginId = "campfire-analytics",
    icon = CampfireIcons.Rounded.Analytics,
    title = "Analytics",
  )

  @Composable
  override fun Content() {
    val events by LivewireAnalytics.events.collectAsState()
    var filter by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }

    val filtered = remember(events, filter) {
      if (filter.isBlank()) {
        events
      } else {
        events.filter { it.matches(filter) }
      }
    }
    val selected = events.find { it.id == selectedId }

    Row(LivewireModifier.fillMaxSize()) {
      // Master list (left side)
      Column(
        LivewireModifier
          .weight(1f)
          .fillMaxHeight(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = LivewireModifier
            .background(LivewireTheme.colorScheme.surfaceContainerHigh)
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        ) {
          Surface(
            modifier = LivewireModifier
              .weight(1f)
              .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 2.dp,
          ) {
            BasicTextField(
              initialValue = filter,
              onValueChange = valueChangeAction { filter = it },
              placeholder = "Filter by event name or parameter…",
              singleLine = true,
              textStyle = LivewireTheme.typography.bodyMedium,
              modifier = LivewireModifier
                .fillMaxWidth()
                .padding(12.dp),
            )
          }

          Spacer(LivewireModifier.width(8.dp))

          Button(
            action = clickAction {
              LivewireAnalytics.clear()
              selectedId = null
            },
            size = ButtonSize.Small,
            style = ButtonStyle.Tonal,
            shapes = ButtonShapes(
              shape = RoundedCornerShape(8.dp),
              pressedShape = CircleShape,
            ),
          ) {
            Icon(imageVector = Icons.Rounded.Delete)
            Text("Clear")
          }
        }

        Column(
          LivewireModifier
            .fillMaxSize()
            .verticalScroll()
            .padding(horizontal = 8.dp),
        ) {
          when {
            events.isEmpty() -> EmptyState {
              Text(
                text = "No events yet — use the app to generate analytics.",
                color = LivewireTheme.colorScheme.onSurfaceVariant,
              )
            }
            filtered.isEmpty() -> EmptyState {
              Text(
                text = "No events match \"$filter\".",
                color = LivewireTheme.colorScheme.onSurfaceVariant,
              )
            }
            else -> {
              Spacer(LivewireModifier.height(8.dp))
              filtered.forEach { event ->
                EventRow(
                  event = event,
                  selected = event.id == selectedId,
                  onClick = clickAction(key = "event_${event.id}") {
                    selectedId = if (selectedId == event.id) null else event.id
                  },
                )
              }
            }
          }
        }
      }

      // Detail pane (right side)
      AnimatedVisibility(
        visible = selected != null,
        modifier = LivewireModifier.fillMaxHeight(),
      ) {
        ResizableSurface(
          anchor = ResizeAnchor.Start,
          initialSize = 420.dp,
          minSize = 280.dp,
          tonalElevation = 2.dp,
          modifier = LivewireModifier.fillMaxHeight(),
        ) {
          selected?.let { event ->
            EventDetailPanel(
              event = event,
              onClose = clickAction { selectedId = null },
            )
          }
        }
      }
    }
  }
}

private fun RecordedEvent.matches(filter: String): Boolean {
  return name.contains(filter, ignoreCase = true) ||
    params.any { (key, value) ->
      key.contains(filter, ignoreCase = true) || value.contains(filter, ignoreCase = true)
    }
}

@Composable
private fun EmptyState(
  modifier: LivewireModifier = LivewireModifier,
  content:
  @Composable @LivewireComposable
  ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        vertical = 100.dp,
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    content()
  }
}

@Composable
private fun EventRow(
  event: RecordedEvent,
  selected: Boolean,
  onClick: ClickAction,
) {
  Surface(
    modifier = LivewireModifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    shape = RoundedCornerShape(8.dp),
    color = if (selected) LivewireTheme.colorScheme.secondaryContainer else null,
    tonalElevation = 1.dp,
    onClick = onClick,

  ) {
    Row(
      LivewireModifier
        .fillMaxWidth()
        .padding(
          horizontal = 12.dp,
          vertical = 8.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(LivewireModifier.weight(1f)) {
        Text(
          text = event.name,
          style = LivewireTheme.typography.titleSmall,
        )
        Text(
          text = "${event.params.size} parameter${if (event.params.size > 1) "s" else ""}",
          style = LivewireTheme.typography.labelSmall,
          color = LivewireTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = eventTimeFormat.format(Date(event.timeMs)),
        style = LivewireTheme.typography.labelSmall,
        color = LivewireTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun EventDetailPanel(
  event: RecordedEvent,
  onClose: ClickAction,
) {
  Column(LivewireModifier.fillMaxSize()) {
    Surface(
      color = LivewireTheme.colorScheme.surfaceContainerHigh,
    ) {
      Row(
        modifier = LivewireModifier
          .fillMaxWidth()
          .height(60.dp)
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = event.name,
          modifier = LivewireModifier.weight(1f),
          style = LivewireTheme.typography.titleMedium,
        )
        SectionButton(action = onClose) { Text("Close") }
      }
    }

    Column(
      LivewireModifier
        .fillMaxSize()
        .verticalScroll()
        .padding(horizontal = 16.dp),
    ) {
      SectionHeader("Event")
      SegmentedSection(
        title = "",
        rows = listOf(
          "name" to event.name,
          "time" to eventTimeFormat.format(Date(event.timeMs)),
          "parameters" to event.params.size.toString(),
        ),
      )

      SectionHeader("Parameters")
      if (event.params.isEmpty()) {
        Text("This event carries no parameters.", color = Color.Gray)
      } else {
        SegmentedSection(
          title = "",
          rows = event.params.toList().sortedBy { it.first },
        )
      }
    }
  }
}

private val eventTimeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
