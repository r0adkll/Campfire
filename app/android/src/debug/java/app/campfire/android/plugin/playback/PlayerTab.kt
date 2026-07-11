package app.campfire.android.plugin.playback

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import app.campfire.android.plugin.common.LoadingIndicator
import app.campfire.android.plugin.common.SectionHeader
import app.campfire.android.plugin.common.SegmentedSection
import com.livewire.ui.layout.Column
import com.livewire.ui.layout.Row
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.fillMaxHeight
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.height
import com.livewire.ui.modifier.padding
import com.livewire.ui.modifier.verticalScroll
import com.livewire.ui.widget.ResizableSurface
import com.livewire.ui.widget.ResizeAnchor
import com.livewire.ui.widget.Table
import com.livewire.ui.widget.Text

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
