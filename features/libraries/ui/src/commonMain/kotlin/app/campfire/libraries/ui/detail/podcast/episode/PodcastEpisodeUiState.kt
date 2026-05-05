package app.campfire.libraries.ui.detail.podcast.episode

import app.campfire.core.model.PodcastEpisode
import com.slack.circuit.runtime.CircuitUiState

data class PodcastEpisodeUiState(
  val episode: PodcastEpisode,
  val eventSink: (PodcastEpisodeUiEvent) -> Unit,
) : CircuitUiState

sealed interface PodcastEpisodeUiEvent {
  data object PlayClick : PodcastEpisodeUiEvent
}
