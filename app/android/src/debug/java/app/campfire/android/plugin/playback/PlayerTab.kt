package app.campfire.android.plugin.playback

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.livewire.ui.actions.ClickAction
import com.livewire.ui.graphics.CircleShape
import com.livewire.ui.graphics.RoundedCornerShape
import com.livewire.ui.layout.Alignment
import com.livewire.ui.layout.Arrangement
import com.livewire.ui.layout.Column
import com.livewire.ui.layout.Row
import com.livewire.ui.layout.RowScope
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.fillMaxHeight
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.height
import com.livewire.ui.modifier.padding
import com.livewire.ui.modifier.verticalScroll
import com.livewire.ui.widget.Button
import com.livewire.ui.widget.ButtonShapes
import com.livewire.ui.widget.ButtonSize
import com.livewire.ui.widget.ButtonStyle
import com.livewire.ui.widget.HorizontalDivider
import com.livewire.ui.widget.ProgressIndicator
import com.livewire.ui.widget.ProgressIndicatorStyle
import com.livewire.ui.widget.ResizableSurface
import com.livewire.ui.widget.ResizeAnchor
import com.livewire.ui.widget.Spacer
import com.livewire.ui.widget.Surface
import com.livewire.ui.widget.Table
import com.livewire.ui.widget.Text
import com.livewire.ui.widget.TextStyle

/**
 * Split-pane MediaController inspector: a segmented list of the controller state plus
 * the queue/timeline on the left, and a mock player mirroring the app's player view
 * (driven by this plugin's own debug MediaController) in the side panel.
 */
@Composable
internal fun PlayerTab(
  controller: MediaController?,
  artworkLoader: DebugArtworkLoader,
) {
  if (controller == null) {
    LoadingIndicator("Connecting to AudioPlayerService…", LivewireModifier.fillMaxSize())
    return
  }
  val snapshot = rememberControllerSnapshot(controller)
  if (snapshot == null) {
    LoadingIndicator("Waiting for first snapshot…", LivewireModifier.fillMaxSize())
    return
  }

  Row(LivewireModifier.fillMaxSize()) {
    // Left pane: segmented controller info + queue/timeline
    Column(
      LivewireModifier
        .weight(1f)
        .fillMaxHeight()
        .verticalScroll()
        .padding(16.dp),
    ) {
      snapshot.sections.forEach { (title, rows) ->
        SegmentedSection(title, rows)
      }

      SectionHeader("Queue / Timeline (${snapshot.queue.size} items)")
      if (snapshot.queue.isEmpty()) {
        Text("Queue is empty — no session prepared.", color = Color.Gray)
      } else {
        Table(
          columns = listOf("#", "Media ID", "Title", "Duration", "Clipping"),
          rows = snapshot.queue.map { row ->
            listOf(
              if (row.index == snapshot.currentIndex) "▶ ${row.index}" else row.index.toString(),
              row.mediaId,
              row.title,
              row.duration,
              row.clipping,
            )
          },
          modifier = LivewireModifier.fillMaxWidth().height(500.dp),
        )
      }
    }

    // Side panel: mock player view
    ResizableSurface(
      anchor = ResizeAnchor.Start,
      initialSize = 360.dp,
      minSize = 280.dp,
      tonalElevation = 2.dp,
    ) {
      MockPlayer(controller, snapshot, artworkLoader)
    }
  }
}

/**
 * An iOS-settings-style segment: a header followed by a rounded card of
 * label/value rows separated by dividers.
 */
@Composable
internal fun SegmentedSection(
  title: String,
  rows: List<Pair<String, String>>,
) {
  if (title.isNotEmpty()) SectionHeader(title)
  Surface(
    modifier = LivewireModifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    tonalElevation = 3.dp,
  ) {
    Column(LivewireModifier.fillMaxWidth()) {
      rows.forEachIndexed { index, (label, value) ->
        if (index > 0) HorizontalDivider()
        Row(
          LivewireModifier
            .fillMaxWidth()
            .padding(12.dp),
        ) {
          Text(
            text = label,
            modifier = LivewireModifier.weight(1f),
            color = Color.Gray,
            style = TextStyle.BodyMedium,
          )
          Text(
            text = value,
            style = TextStyle.BodyMedium,
          )
        }
      }
    }
  }
}

@Composable
internal fun SectionHeader(
  title: String,
  modifier: LivewireModifier = LivewireModifier,
  trailingAction: @Composable () -> Unit = {},
) {
  Row(
    modifier = modifier
      .height(56.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = TextStyle.TitleMedium,
      modifier = LivewireModifier.weight(1f),
    )
    trailingAction()
  }
}

/**
 * The standard loading state: a centered circular spinner with a short label
 * beneath it. Size via [modifier] — fillMaxSize() for whole-pane loads.
 */
@Composable
internal fun LoadingIndicator(
  message: String,
  modifier: LivewireModifier = LivewireModifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    ProgressIndicator(style = ProgressIndicatorStyle.Circular)
    Spacer(LivewireModifier.height(8.dp))
    Text(
      text = message,
      style = TextStyle.BodySmall,
      color = Color.Gray,
    )
  }
}

/**
 * The standard section action button: extra-small, tonal, rounded-rect that
 * rounds to a circle when pressed. Use for Clear / Pause all / Resume all etc.
 */
@Composable
internal fun SectionButton(
  action: ClickAction,
  modifier: LivewireModifier = LivewireModifier,
  content: @Composable RowScope.() -> Unit,
) {
  Button(
    action = action,
    modifier = modifier,
    size = ButtonSize.ExtraSmall,
    style = ButtonStyle.Tonal,
    shapes = ButtonShapes(
      shape = RoundedCornerShape(8.dp),
      pressedShape = CircleShape,
    ),
    content = content,
  )
}
