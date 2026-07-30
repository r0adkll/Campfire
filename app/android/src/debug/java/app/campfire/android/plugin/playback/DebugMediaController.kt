// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.plugin.playback

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Connection hint identifying the Livewire debug controller to the session. */
const val CONNECTION_HINT_LIVEWIRE = "app.campfire.debug.livewire"

/**
 * A [MediaBrowser] owned entirely by the plugin — never a controller shared with the
 * integrating app's UI. Scoped to the plugin's composition: connects when the plugin
 * is selected on the host and releases when it is deselected or the host disconnects.
 * Connecting binds (and may start) the app's [MediaSessionService].
 *
 * A browser rather than a plain controller so the Auto tab can exercise the session's
 * real MediaLibrarySession.Callback browse path.
 */
@Composable
fun rememberDebugMediaBrowser(
  context: Context,
  sessionServiceClass: Class<out MediaSessionService>,
): MediaBrowser? {
  var browser by remember { mutableStateOf<MediaBrowser?>(null) }
  DisposableEffect(Unit) {
    val future = MediaBrowser.Builder(
      context,
      SessionToken(context, ComponentName(context, sessionServiceClass)),
    )
      .setConnectionHints(bundleOf(CONNECTION_HINT_LIVEWIRE to true))
      .buildAsync()
    future.addListener(
      { browser = runCatching { future.get() }.getOrNull() },
      ContextCompat.getMainExecutor(context),
    )
    onDispose {
      browser = null
      Handler(Looper.getMainLooper()).post {
        MediaBrowser.releaseFuture(future)
      }
    }
  }
  return browser
}

/**
 * Awaits a media3 [ListenableFuture] without pulling in kotlinx-coroutines-guava.
 */
suspend fun <T> ListenableFuture<T>.awaitFuture(): T = suspendCancellableCoroutine { continuation ->
  addListener(
    {
      try {
        continuation.resume(get())
      } catch (e: Exception) {
        continuation.resumeWithException(e)
      }
    },
    { runnable -> runnable.run() },
  )
  continuation.invokeOnCancellation { cancel(false) }
}

data class ControllerSnapshot(
  val playbackState: String,
  val playWhenReady: Boolean,
  val isPlaying: Boolean,
  val suppressionReason: String,
  val playbackSpeed: Float,
  val volume: Float,
  val repeatMode: String,
  val shuffleEnabled: Boolean,
  val positionMs: Long,
  val bufferedPositionMs: Long,
  val durationMs: Long,
  val currentIndex: Int,
  val mediaItemCount: Int,
  val error: String?,
  val title: String?,
  val artist: String?,
  val artworkUri: String?,
  val customCommands: List<String>,
  val metadata: List<Pair<String, String>>,
  val queue: List<QueueRow>,
) {
  /** Grouped label/value sections for the segmented list on the Player tab. */
  val sections: List<Pair<String, List<Pair<String, String>>>>
    get() = listOf(
      "Playback" to listOf(
        "playbackState" to playbackState,
        "playWhenReady" to playWhenReady.toString(),
        "isPlaying" to isPlaying.toString(),
        "suppressionReason" to suppressionReason,
        "playerError" to (error ?: "none"),
      ),
      "Position" to listOf(
        "position" to positionMs.asPlayerTime(),
        "bufferedPosition" to bufferedPositionMs.asPlayerTime(),
        "duration" to durationMs.asPlayerTime(),
        "currentMediaItem" to "$currentIndex of $mediaItemCount",
      ),
      "Settings" to listOf(
        "playbackSpeed" to "${playbackSpeed}x",
        "volume" to volume.toString(),
        "repeatMode" to repeatMode,
        "shuffleEnabled" to shuffleEnabled.toString(),
      ),
      "Metadata" to metadata,
    )
}

data class QueueRow(
  val index: Int,
  val mediaId: String,
  val title: String,
  val duration: String,
  val clipping: String,
)

/**
 * Polls a [ControllerSnapshot] from [controller] on the main thread while in composition.
 */
@Composable
fun rememberControllerSnapshot(controller: MediaController?): ControllerSnapshot? {
  var snapshot by remember { mutableStateOf<ControllerSnapshot?>(null) }
  LaunchedEffect(controller) {
    if (controller == null) {
      snapshot = null
      return@LaunchedEffect
    }
    while (true) {
      snapshot = withContext(Dispatchers.Main) { controller.snapshot() }
      delay(500L)
    }
  }
  return snapshot
}

private fun MediaController.snapshot(): ControllerSnapshot {
  return ControllerSnapshot(
    playbackState = when (playbackState) {
      Player.STATE_IDLE -> "IDLE"
      Player.STATE_BUFFERING -> "BUFFERING"
      Player.STATE_READY -> "READY"
      Player.STATE_ENDED -> "ENDED"
      else -> "UNKNOWN($playbackState)"
    },
    playWhenReady = playWhenReady,
    isPlaying = isPlaying,
    suppressionReason = when (playbackSuppressionReason) {
      Player.PLAYBACK_SUPPRESSION_REASON_NONE -> "NONE"
      Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS -> "TRANSIENT_AUDIO_FOCUS_LOSS"
      else -> "OTHER($playbackSuppressionReason)"
    },
    playbackSpeed = playbackParameters.speed,
    volume = volume,
    repeatMode = when (repeatMode) {
      Player.REPEAT_MODE_OFF -> "OFF"
      Player.REPEAT_MODE_ONE -> "ONE"
      Player.REPEAT_MODE_ALL -> "ALL"
      else -> "UNKNOWN($repeatMode)"
    },
    shuffleEnabled = shuffleModeEnabled,
    positionMs = currentPosition,
    bufferedPositionMs = bufferedPosition,
    durationMs = duration,
    currentIndex = currentMediaItemIndex,
    mediaItemCount = mediaItemCount,
    error = playerError?.let { "${it.errorCodeName}: ${it.message}" },
    title = mediaMetadata.title?.toString(),
    artist = mediaMetadata.artist?.toString(),
    artworkUri = mediaMetadata.artworkUri?.toString(),
    customCommands = availableSessionCommands.commands
      .filter { it.commandCode == SessionCommand.COMMAND_CODE_CUSTOM }
      .map { it.customAction },
    metadata = mediaMetadata.asFieldPairs(),
    queue = (0 until mediaItemCount).map { index ->
      val item = getMediaItemAt(index)
      QueueRow(
        index = index,
        mediaId = item.mediaId,
        title = item.mediaMetadata.title?.toString() ?: "—",
        duration = item.mediaMetadata.durationMs?.asPlayerTime() ?: "—",
        clipping = item.clippingConfiguration.let { clip ->
          if (clip.startPositionMs == 0L && clip.endPositionMs == C.TIME_END_OF_SOURCE) {
            "—"
          } else {
            "${clip.startPositionMs.asPlayerTime()} → ${clip.endPositionMs.asPlayerTime()}"
          }
        },
      )
    },
  )
}

private fun MediaMetadata.asFieldPairs(): List<Pair<String, String>> = listOf(
  "title" to (title?.toString() ?: "null"),
  "artist" to (artist?.toString() ?: "null"),
  "albumTitle" to (albumTitle?.toString() ?: "null"),
  "subtitle" to (subtitle?.toString() ?: "null"),
  "mediaType" to mediaTypeName(mediaType),
  "durationMs" to (durationMs?.asPlayerTime() ?: "null"),
  "artworkUri" to artworkDescription(),
  "extras" to (extras?.keySet()?.joinToString() ?: "null"),
)

/**
 * External controllers (notification, Android Auto) can only render artwork they can
 * read: a content:// URI from CoverContentProvider. A raw server URL here is a bug.
 */
private fun MediaMetadata.artworkDescription(): String {
  val uri = artworkUri ?: return "null"
  return when (uri.scheme) {
    "content" -> "$uri (cross-process OK)"
    "http", "https" -> "$uri (WARNING: raw URL, external controllers cannot authenticate)"
    else -> uri.toString()
  }
}

private fun mediaTypeName(type: Int?): String = when (type) {
  null -> "null"
  MediaMetadata.MEDIA_TYPE_MIXED -> "MIXED"
  MediaMetadata.MEDIA_TYPE_MUSIC -> "MUSIC"
  MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER -> "AUDIO_BOOK_CHAPTER"
  MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE -> "PODCAST_EPISODE"
  else -> type.toString()
}

internal fun Long.asPlayerTime(): String = when (this) {
  C.TIME_UNSET -> "UNSET"
  C.TIME_END_OF_SOURCE -> "END_OF_SOURCE"
  else -> "$milliseconds ($this ms)"
}
