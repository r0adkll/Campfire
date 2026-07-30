// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Bookmark
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Session
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.sessions.api.QueuedEntry
import com.r0adkll.swatchbuckler.compose.Theme
import kotlin.time.Duration

@Immutable
data class PlaybackUiState(
  val session: Session?,
  val playerState: PlayerUiState,
  val queueState: QueueUiState,
  val syncUiState: SyncUiState,
  val themeState: ThemeUiState,
  val validation: LibraryItemValidation,
  val playbackHistoryEnabled: Boolean,
  val eventSink: (PlaybackUiEvent) -> Unit,
)

@Immutable
data class PlayerUiState(
  val time: Duration,
  val bookTime: Duration,
  val bookTimeEnabled: Boolean,
  val duration: Duration,
  val wavySliderEnabled: Boolean,
  val metadata: Metadata,
  val state: AudioPlayer.State,
  val speed: Float,
  val timer: RunningTimer?,
  val error: Throwable?,
  val eventSink: (PlayerUiEvent) -> Unit,
)

@Immutable
data class QueueUiState(
  val queue: List<QueuedEntry>,
  val eventSink: (QueueUiEvent) -> Unit,
)

@Immutable
data class SyncUiState(
  val mediaProgress: MediaProgress?,
  val availableSync: AvailableSync?,
  val eventSink: (SyncUiEvent) -> Unit,
)

@Immutable
data class AvailableSync(
  val itemId: LibraryItemId,
  val currentTime: Duration,
  val targetTime: Duration,
  val syncTimeInMillis: Long,
  val targetChapterTitle: String? = null,
)

@Immutable
data class ThemeUiState(
  val dynamicThemingEnabled: Boolean,
  val theme: Theme?,
)

@Stable
sealed interface PlaybackUiEvent {
  data object ClearSession : PlaybackUiEvent
  data object StartSession : PlaybackUiEvent
}

@Stable
sealed interface PlayerUiEvent {
  data object PlayPauseClick : PlayerUiEvent
  data object NextClick : PlayerUiEvent
  data object PreviousClick : PlayerUiEvent
  data object RewindClick : PlayerUiEvent
  data object FastForwardClick : PlayerUiEvent

  sealed interface Seek : PlayerUiEvent {
    data class Percent(val percent: Float) : Seek
    data class Position(val position: Duration) : Seek
  }

  data class BookmarkSelected(val bookmark: Bookmark) : PlayerUiEvent
  data class TimerSelected(val timer: PlaybackTimer) : PlayerUiEvent
  data object ClearTimer : PlayerUiEvent
  data class ChapterSelected(val chapter: Chapter) : PlayerUiEvent
  data class AudioTrackSelected(val audioTrack: AudioTrack) : PlayerUiEvent
}

@Stable
sealed interface QueueUiEvent {
  data class ReorderItem(
    val fromKey: String,
    val toKey: String,
  ) : QueueUiEvent
  data object ReorderStopped : QueueUiEvent

  data class QueueItemClick(val entry: QueuedEntry) : QueueUiEvent
  data class RemoveQueueItem(val entry: QueuedEntry) : QueueUiEvent
  data object ClearQueue : QueueUiEvent
}

@Stable
sealed interface SyncUiEvent {
  data object Sync : SyncUiEvent
}
