package app.campfire.podcasts.ui.add

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import app.campfire.podcasts.api.PodcastDraft
import app.campfire.podcasts.api.PodcastSearchResult
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class AddPodcastUiState(
  val textFieldState: TextFieldState,
  val searchState: SearchState,
  val canAddPodcasts: Boolean,
  val eventSink: (AddPodcastUiEvent) -> Unit,
) : CircuitUiState

sealed interface SearchState {
  data object Idle : SearchState
  data object Loading : SearchState
  data object Forbidden : SearchState
  data object Error : SearchState
  data class NoResults(val query: String) : SearchState
  data class Results(val hits: List<PodcastSearchResult>) : SearchState
  data class FeedPreview(val draft: PodcastDraft) : SearchState
}

sealed interface AddPodcastUiEvent : CircuitUiEvent {
  data object Back : AddPodcastUiEvent
  data class ResultTapped(val result: PodcastSearchResult) : AddPodcastUiEvent
  data class PreviewTapped(val draft: PodcastDraft) : AddPodcastUiEvent
  data object Retry : AddPodcastUiEvent
}
