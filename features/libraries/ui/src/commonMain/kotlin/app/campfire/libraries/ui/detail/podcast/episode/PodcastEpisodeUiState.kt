package app.campfire.libraries.ui.detail.podcast.episode

import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.SessionUiState
import com.slack.circuit.runtime.CircuitUiState
import kotlin.time.Duration

data class PodcastEpisodeUiState(
  val episode: PodcastEpisode,
  val progress: MediaProgress?,
  val playbackSpeed: Float,
  val isPlaying: Boolean,
  val isQueued: Boolean,
  val hasSession: Boolean,
  val sessionState: SessionUiState,
  val eventSink: (PodcastEpisodeUiEvent) -> Unit,
) : CircuitUiState

sealed interface PodcastEpisodeUiEvent {
  data object PlayClick : PodcastEpisodeUiEvent
  data object MarkFinished : PodcastEpisodeUiEvent
  data object MarkNotFinished : PodcastEpisodeUiEvent
  data object DiscardProgress : PodcastEpisodeUiEvent
  data class Seek(val position: Duration) : PodcastEpisodeUiEvent
}
