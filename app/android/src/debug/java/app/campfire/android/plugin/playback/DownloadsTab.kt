// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.plugin.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import app.campfire.android.plugin.common.LoadingIndicator
import app.campfire.android.plugin.common.LogRow
import app.campfire.android.plugin.common.SectionButton
import app.campfire.android.plugin.common.SectionHeader
import app.campfire.android.plugin.common.SegmentedSection
import app.campfire.android.plugin.playback.icons.DeleteForever
import app.campfire.android.plugin.playback.icons.Pause
import app.campfire.android.plugin.playback.icons.PlayArrow
import com.livewire.ui.actions.clickAction
import com.livewire.ui.graphics.RoundedCornerShape
import com.livewire.ui.layout.Alignment
import com.livewire.ui.layout.Column
import com.livewire.ui.layout.Row
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.fillMaxHeight
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.padding
import com.livewire.ui.modifier.verticalScroll
import com.livewire.ui.theme.LivewireTheme
import com.livewire.ui.widget.Icon
import com.livewire.ui.widget.IconButton
import com.livewire.ui.widget.ProgressIndicator
import com.livewire.ui.widget.ResizableSurface
import com.livewire.ui.widget.ResizeAnchor
import com.livewire.ui.widget.Surface
import com.livewire.ui.widget.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Pure-media3 DownloadService inspector: the DownloadManager state and per-download
 * cards with retry/remove controls on the left, and the live DownloadManager.Listener
 * event log in the side panel. The only app-provided inputs are the DownloadManager
 * instance and the concrete DownloadService class, both injected by the wiring —
 * nothing here knows about the app's download layer.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun DownloadsTab(
  downloadManager: DownloadManager,
  downloadServiceClass: Class<out DownloadService>,
  context: Context,
) {
  var downloads by remember { mutableStateOf<List<DownloadRow>?>(null) }
  var stats by remember { mutableStateOf<List<Pair<String, String>>?>(null) }

  LaunchedEffect(Unit) {
    while (true) {
      withContext(Dispatchers.IO) {
        downloads = loadDownloadRows(downloadManager)
        stats = listOf(
          "downloadsPaused" to downloadManager.downloadsPaused.toString(),
          "activeDownloads" to downloadManager.currentDownloads.size.toString(),
          "maxParallelDownloads" to downloadManager.maxParallelDownloads.toString(),
          "waitingForRequirements" to downloadManager.isWaitingForRequirements.toString(),
          "notMetRequirements" to downloadManager.notMetRequirements.toString(),
        )
      }
      delay(1_000L)
    }
  }

  val currentStats = stats
  val currentDownloads = downloads
  if (currentStats == null || currentDownloads == null) {
    LoadingIndicator("Reading download index…", LivewireModifier.fillMaxSize())
    return
  }

  Row(LivewireModifier.fillMaxSize()) {
    // Left pane: manager state + download cards
    Column(
      LivewireModifier
        .weight(1f)
        .fillMaxHeight()
        .verticalScroll()
        .padding(horizontal = 16.dp),
    ) {
      SectionHeader("DownloadManager")
      SegmentedSection("State", currentStats)

      SectionHeader("Downloads (${currentDownloads.size})") {
        SectionButton(
          action = mainAction {
            DownloadService.sendPauseDownloads(context, downloadServiceClass, false)
          },
          modifier = LivewireModifier.padding(2.dp),
        ) {
          Icon(Pause)
          Text("Pause all")
        }
        SectionButton(
          action = mainAction {
            DownloadService.sendResumeDownloads(context, downloadServiceClass, false)
          },
          modifier = LivewireModifier.padding(2.dp),
        ) {
          Icon(PlayArrow)
          Text("Resume all")
        }
        SectionButton(
          action = mainAction {
            DownloadService.sendRemoveAllDownloads(context, downloadServiceClass, false)
          },
          modifier = LivewireModifier.padding(2.dp),
        ) {
          Icon(DeleteForever)
          Text("Delete all")
        }
      }
      if (currentDownloads.isEmpty()) {
        Text("No downloads in the index.", color = Color.Gray)
      } else {
        currentDownloads.forEach { download ->
          DownloadCard(
            download = download,
            onRetry = mainAction(key = "retry_${download.id}") {
              download.request?.let { request ->
                DownloadService.sendAddDownload(context, downloadServiceClass, request, false)
              }
            },
            onRemove = mainAction(key = "remove_${download.id}") {
              DownloadService.sendRemoveDownload(context, downloadServiceClass, download.id, false)
            },
          )
        }
      }
    }

    // Side panel: live DownloadManager.Listener event log
    ResizableSurface(
      anchor = ResizeAnchor.Start,
      initialSize = 420.dp,
      minSize = 300.dp,
      tonalElevation = 2.dp,
    ) {
      DownloadLogPanel()
    }
  }
}

@Composable
private fun DownloadCard(
  download: DownloadRow,
  onRetry: com.livewire.ui.actions.ClickAction,
  onRemove: com.livewire.ui.actions.ClickAction,
) {
  Surface(
    modifier = LivewireModifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    shape = RoundedCornerShape(8.dp),
    tonalElevation = 1.dp,
  ) {
    Column(
      LivewireModifier
        .fillMaxWidth()
        .padding(8.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(LivewireModifier.weight(1f)) {
          Text(
            text = download.id,
            style = LivewireTheme.typography.titleSmall,
          )
          Text(
            text = download.uri,
            style = LivewireTheme.typography.bodySmall,
            color = Color.Gray,
          )
        }
        if (download.state == Download.STATE_FAILED) {
          IconButton(action = onRetry) {
            Icon(Icons.Rounded.Refresh)
          }
        }
        IconButton(action = onRemove) {
          Icon(Icons.Rounded.Delete)
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = download.state.asDownloadStateName(),
          style = LivewireTheme.typography.labelMedium,
          color = download.state.asDownloadStateColor(),
        )
        Text(
          text = "  ${download.progressText}",
          style = LivewireTheme.typography.labelMedium,
          color = Color.Gray,
        )
      }
      if (download.state == Download.STATE_DOWNLOADING) {
        ProgressIndicator(
          modifier = LivewireModifier.fillMaxWidth().padding(vertical = 4.dp),
          progress = download.percent?.let { it / 100f },
        )
      }
      if (download.failure != null) {
        Text(
          text = download.failure,
          style = LivewireTheme.typography.bodySmall,
          color = Color.Red,
        )
      }
    }
  }
}

@Composable
private fun DownloadLogPanel() {
  val events by DownloadDebugCollector.events.collectAsState()

  Column(
    LivewireModifier
      .fillMaxSize()
      .padding(16.dp),
  ) {
    Row(
      LivewireModifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "DownloadManager events",
        modifier = LivewireModifier.weight(1f),
        style = LivewireTheme.typography.titleMedium,
      )
      SectionButton(
        action = clickAction { DownloadDebugCollector.clear() },
      ) { Text("Clear") }
    }

    if (events.isEmpty()) {
      Text(
        text = "No events yet — queue, pause, or remove a download.",
        color = Color.Gray,
        style = LivewireTheme.typography.bodySmall,
      )
    } else {
      val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
      Column(
        LivewireModifier
          .fillMaxSize()
          .verticalScroll(),
      ) {
        events.forEach { event ->
          LogRow(
            time = timeFormat.format(Date(event.timeMs)),
            type = event.type,
            typeColor = event.type.asDownloadEventColor(),
            source = null,
            details = event.details,
          )
        }
      }
    }
  }
}

private data class DownloadRow(
  val id: String,
  val uri: String,
  val state: Int,
  val percent: Float?,
  val progressText: String,
  val failure: String?,
  val request: androidx.media3.exoplayer.offline.DownloadRequest?,
)

private fun loadDownloadRows(downloadManager: DownloadManager): List<DownloadRow> {
  val rows = mutableListOf<DownloadRow>()
  downloadManager.downloadIndex.getDownloads().use { cursor ->
    while (cursor.moveToNext()) {
      val download = cursor.download
      val percent = download.percentDownloaded.takeIf { it != C.PERCENTAGE_UNSET.toFloat() }
      val total = download.contentLength.takeIf { it != C.LENGTH_UNSET.toLong() }
      rows += DownloadRow(
        id = download.request.id,
        uri = download.request.uri.toString(),
        state = download.state,
        percent = percent,
        progressText = buildString {
          append(percent?.let { "${it.toInt()}%" } ?: "?%")
          append(" · ")
          append(download.bytesDownloaded.asByteSize())
          append(" / ")
          append(total?.asByteSize() ?: "?")
        },
        failure = if (download.state == Download.STATE_FAILED) {
          "failureReason=${download.failureReason}"
        } else {
          null
        },
        request = download.request,
      )
    }
  }
  rows.sortWith(compareBy({ it.state.asDownloadSortRank() }, { it.id }))
  return rows
}

/**
 * Sort order for the downloads list: active work first, queued next, terminal
 * states after, and the (usually large) completed pile last.
 */
private fun Int.asDownloadSortRank(): Int = when (this) {
  Download.STATE_DOWNLOADING -> 0
  Download.STATE_RESTARTING -> 1
  Download.STATE_QUEUED -> 2
  Download.STATE_FAILED -> 3
  Download.STATE_STOPPED -> 4
  Download.STATE_REMOVING -> 5
  Download.STATE_COMPLETED -> 6
  else -> 7
}

private fun Int.asDownloadStateColor(): Color = when (this) {
  Download.STATE_DOWNLOADING -> Color.Cyan
  Download.STATE_COMPLETED -> Color.Green
  Download.STATE_FAILED -> Color.Red
  Download.STATE_QUEUED -> Color.Yellow
  Download.STATE_REMOVING, Download.STATE_RESTARTING -> Color.Magenta
  else -> Color.LightGray
}

private fun String.asDownloadEventColor(): Color = when (this) {
  "downloadChanged" -> Color.Cyan
  "downloadRemoved" -> Color.Magenta
  "pausedChanged", "waitingForRequirements", "requirements" -> Color.Yellow
  "initialized", "idle" -> Color.Green
  else -> Color.LightGray
}

private fun Long.asByteSize(): String = when {
  this < 0L -> "?"
  this < 1024L -> "$this B"
  this < 1024L * 1024L -> "%.1f KB".format(this / 1024f)
  this < 1024L * 1024L * 1024L -> "%.1f MB".format(this / (1024f * 1024f))
  else -> "%.2f GB".format(this / (1024f * 1024f * 1024f))
}
