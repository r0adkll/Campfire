// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.plugin.playback

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import app.campfire.android.plugin.common.SectionButton
import com.livewire.ui.graphics.CircleShape
import com.livewire.ui.graphics.CornerSize
import com.livewire.ui.graphics.RoundedCornerShape
import com.livewire.ui.layout.Alignment
import com.livewire.ui.layout.Arrangement
import com.livewire.ui.layout.Column
import com.livewire.ui.layout.Row
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.clip
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.height
import com.livewire.ui.modifier.padding
import com.livewire.ui.modifier.size
import com.livewire.ui.modifier.verticalScroll
import com.livewire.ui.theme.LivewireTheme
import com.livewire.ui.widget.ButtonSize
import com.livewire.ui.widget.Icon
import com.livewire.ui.widget.IconButton
import com.livewire.ui.widget.IconButtonShapes
import com.livewire.ui.widget.IconButtonStyle
import com.livewire.ui.widget.Image
import com.livewire.ui.widget.ProgressIndicator
import com.livewire.ui.widget.ProgressIndicatorStyle
import com.livewire.ui.widget.Slider
import com.livewire.ui.widget.Spacer
import com.livewire.ui.widget.Text

/**
 * A mock of the app's player view driven entirely by the plugin's debug
 * MediaController — artwork, transport controls, scrubber, and the custom
 * session commands (speed cycle, sleep timer) external controllers use.
 */
@Composable
internal fun MockPlayer(
  controller: MediaController,
  snapshot: ControllerSnapshot,
  artworkLoader: DebugArtworkLoader,
) {
  Column(
    LivewireModifier
      .fillMaxSize()
      .verticalScroll()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Artwork
    when (val artwork = rememberArtworkState(artworkLoader, snapshot.artworkUri)) {
      ArtworkState.Loading -> Column(
        LivewireModifier.size(280.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        ProgressIndicator(style = ProgressIndicatorStyle.Circular)
      }
      is ArtworkState.Loaded -> Image(
        imageData = artwork.bytes,
        contentDescription = snapshot.title,
        modifier = LivewireModifier
          .size(280.dp)
          .clip(RoundedCornerShape(16.dp)),
      )
      ArtworkState.Missing -> Icon(
        imageVector = Icons.Rounded.Album,
        modifier = LivewireModifier.size(180.dp),
        tint = Color.Gray,
      )
    }

    Spacer(LivewireModifier.padding(8.dp))
    Text(
      text = snapshot.title ?: "Nothing playing",
      style = LivewireTheme.typography.titleMedium,
    )
    Text(
      text = snapshot.artist ?: "—",
      style = LivewireTheme.typography.bodyMedium,
      color = Color.Gray,
    )
    Spacer(LivewireModifier.height(16.dp))

    // Scrubber
    val durationMs = snapshot.durationMs.takeIf { it > 0L }
    Slider(
      value = if (durationMs != null && snapshot.positionMs >= 0L) {
        (snapshot.positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
      } else {
        0f
      },
      onValueChange = mainFloatAction { fraction ->
        val duration = controller.duration
        if (duration > 0L) controller.seekTo((fraction * duration).toLong())
      },
      modifier = LivewireModifier.fillMaxWidth(),
      enabled = durationMs != null,
    )
    Row(LivewireModifier.fillMaxWidth()) {
      Text(snapshot.positionMs.asClockTime(), style = LivewireTheme.typography.labelSmall, color = Color.Gray)
      Spacer(LivewireModifier.weight(1f))
      Text(snapshot.durationMs.asClockTime(), style = LivewireTheme.typography.labelSmall, color = Color.Gray)
    }

    Spacer(LivewireModifier.height(16.dp))

    // Transport controls, mirroring the app's player layout
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpacedBy(4.dp),
    ) {
      IconButton(
        action = mainAction { controller.seekBack() },
        size = ButtonSize.Medium,
        style = IconButtonStyle.Tonal,
        shapes = IconButtonShapes(
          shape = RoundedCornerShape(
            topEnd = CornerSize(8.dp),
            bottomEnd = CornerSize(8.dp),
            topStart = CornerSize(50),
            bottomStart = CornerSize(50),
          ),
          pressedShape = CircleShape,
        ),
      ) {
        Icon(Icons.Rounded.Replay10)
      }
      IconButton(
        action = mainAction { controller.seekToPrevious() },
        size = ButtonSize.Medium,
        style = IconButtonStyle.Tonal,
        shapes = IconButtonShapes(
          shape = RoundedCornerShape(8.dp),
          pressedShape = CircleShape,
        ),
      ) {
        Icon(Icons.Rounded.SkipPrevious)
      }
      IconButton(
        action = mainAction { if (controller.playWhenReady) controller.pause() else controller.play() },
        size = ButtonSize.Medium,
        style = IconButtonStyle.Filled,
        shapes = IconButtonShapes(
          shape = RoundedCornerShape(8.dp),
          pressedShape = CircleShape,
        ),
        modifier = LivewireModifier,

      ) {
        Icon(if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow)
      }
      IconButton(
        action = mainAction { controller.seekToNext() },
        size = ButtonSize.Medium,
        style = IconButtonStyle.Tonal,
        shapes = IconButtonShapes(
          shape = RoundedCornerShape(8.dp),
          pressedShape = CircleShape,
        ),
      ) {
        Icon(Icons.Rounded.SkipNext)
      }
      IconButton(
        action = mainAction { controller.seekForward() },
        size = ButtonSize.Medium,
        style = IconButtonStyle.Tonal,
        shapes = IconButtonShapes(
          shape = RoundedCornerShape(
            topStart = CornerSize(8.dp),
            bottomStart = CornerSize(8.dp),
            topEnd = CornerSize(50),
            bottomEnd = CornerSize(50),
          ),
          pressedShape = CircleShape,
        ),
      ) {
        Icon(Icons.Rounded.Forward30)
      }
    }

    Spacer(LivewireModifier.height(16.dp))

    // The session's advertised custom commands, discovered from the connection —
    // sent with empty args, the same path external controllers use
    if (snapshot.customCommands.isNotEmpty()) {
      Column(
        modifier = LivewireModifier
          .padding(horizontal = 32.dp),
      ) {
        snapshot.customCommands.forEach { customAction ->
          SectionButton(
            action = mainAction(key = "custom_$customAction") {
              controller.sendCustomCommand(SessionCommand(customAction, Bundle.EMPTY), Bundle.EMPTY)
            },
            modifier = LivewireModifier.fillMaxWidth(),
          ) { Text(customAction.substringAfterLast('.')) }
        }
      }
    }
  }
}

/**
 * Loads artwork bytes for the mock player. Provided by the integrating app so it can
 * route through its own image pipeline (auth headers, content providers, caching).
 * Return a reasonably sized, compressed image — it is sent over the wire on each load.
 */
fun interface DebugArtworkLoader {
  suspend fun load(uri: String): ByteArray?
}

private sealed interface ArtworkState {
  data object Loading : ArtworkState
  data object Missing : ArtworkState
  class Loaded(val bytes: ByteArray) : ArtworkState
}

/**
 * Loads (and re-loads when the uri changes) the current artwork through the
 * integrating app's [DebugArtworkLoader].
 */
@Composable
private fun rememberArtworkState(artworkLoader: DebugArtworkLoader, artworkUri: String?): ArtworkState {
  var state by remember {
    mutableStateOf<ArtworkState>(if (artworkUri == null) ArtworkState.Missing else ArtworkState.Loading)
  }
  LaunchedEffect(artworkUri) {
    if (artworkUri == null) {
      state = ArtworkState.Missing
      return@LaunchedEffect
    }
    state = ArtworkState.Loading
    state = runCatching { artworkLoader.load(artworkUri) }.getOrNull()
      ?.let { ArtworkState.Loaded(it) }
      ?: ArtworkState.Missing
  }
  return state
}

private fun Long.asClockTime(): String {
  if (this < 0L) return "--:--"
  val totalSeconds = this / 1000
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) {
    "%d:%02d:%02d".format(hours, minutes, seconds)
  } else {
    "%d:%02d".format(minutes, seconds)
  }
}
