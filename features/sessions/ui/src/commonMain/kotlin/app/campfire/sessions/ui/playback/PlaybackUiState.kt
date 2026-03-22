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
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Session
import app.campfire.libraries.api.LibraryItemValidation
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
  val duration: Duration,
  val metadata: Metadata,
  val state: AudioPlayer.State,
  val speed: Float,
  val timer: RunningTimer?,
  val eventSink: (PlayerUiEvent) -> Unit,
)

@Immutable
data class QueueUiState(
  val queue: List<LibraryItem>,
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
    val fromItemId: LibraryItemId,
    val toItemId: LibraryItemId,
  ) : QueueUiEvent
  data object ReorderStopped : QueueUiEvent

  data class QueueItemClick(val item: LibraryItem) : QueueUiEvent
  data class RemoveQueueItem(val item: LibraryItem) : QueueUiEvent
  data object ClearQueue : QueueUiEvent
}

@Stable
sealed interface SyncUiEvent {
  data object Sync : SyncUiEvent
}
