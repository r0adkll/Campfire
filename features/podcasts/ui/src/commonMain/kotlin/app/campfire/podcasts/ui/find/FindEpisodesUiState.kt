package app.campfire.podcasts.ui.find

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import app.campfire.podcasts.api.RemotePodcastEpisode
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class FindEpisodesUiState(
  val textFieldState: TextFieldState,
  val feedState: FeedState,
  val selectedEnclosureUrls: Set<String>,
  val isQueuingDownload: Boolean,
  val canBrowseFeed: Boolean,
  val eventSink: (FindEpisodesUiEvent) -> Unit,
) : CircuitUiState

sealed interface FeedState {
  data object Loading : FeedState
  data object Forbidden : FeedState
  data object Error : FeedState
  data object NoFeedUrl : FeedState
  data class Loaded(val episodes: List<FeedEpisodeRow>) : FeedState
}

@Immutable
data class FeedEpisodeRow(
  val episode: RemotePodcastEpisode,
  val isAlreadyDownloaded: Boolean,
  val isQueued: Boolean = false,
)

sealed interface FindEpisodesUiEvent : CircuitUiEvent {
  data object Back : FindEpisodesUiEvent
  data class ToggleSelection(val episode: RemotePodcastEpisode) : FindEpisodesUiEvent
  data object ClearSelection : FindEpisodesUiEvent
  data object DownloadSelected : FindEpisodesUiEvent
  data object Retry : FindEpisodesUiEvent
}
