package app.campfire.home.ui

import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.home.api.model.Shelf
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class HomeUiState(
  val homeFeed: HomeFeed,
  val eventSink: (HomeUiEvent) -> Unit,
) : CircuitUiState

sealed interface HomeFeed {
  data object Loading : HomeFeed
  data class Loaded(val shelves: List<Shelf<*>>) : HomeFeed
  data object Error : HomeFeed
}

sealed interface HomeUiEvent : CircuitUiEvent {
  data class OpenLibraryItem(val item: LibraryItem) : HomeUiEvent
  data class OpenSeries(val series: Series) : HomeUiEvent
  data class OpenAuthor(val author: Author) : HomeUiEvent
}
