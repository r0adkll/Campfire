package app.campfire.home.ui

import androidx.compose.runtime.Stable
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Series
import app.campfire.home.api.HomeFeedResponse
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.ImmutableMap

@Stable
data class HomeUiState(
  val homeFeed: HomeFeedResponse,
  val offlineStates: ImmutableMap<LibraryItemId, OfflineDownload>,
  val progressStates: ImmutableMap<LibraryItemId, MediaProgress>,
  val eventSink: (HomeUiEvent) -> Unit,
) : CircuitUiState

sealed interface HomeUiEvent : CircuitUiEvent {
  data class OpenLibraryItem(
    val item: LibraryItem,
    val sharedTransitionKey: String,
  ) : HomeUiEvent
  data class OpenSeries(val series: Series) : HomeUiEvent
  data class OpenAuthor(val author: Author) : HomeUiEvent
}
