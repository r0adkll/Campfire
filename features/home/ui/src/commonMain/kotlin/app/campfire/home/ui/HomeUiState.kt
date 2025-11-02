package app.campfire.home.ui

import androidx.compose.runtime.Stable
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Series
import app.campfire.core.model.ShelfEntity
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.model.Shelf
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList

@Stable
data class HomeUiState(
  val homeFeed: FeedResponse<out PersistentList<UiShelf<ShelfEntity>>>,
  val offlineStates: ImmutableMap<LibraryItemId, OfflineDownload>,
  val progressStates: ImmutableMap<LibraryItemId, MediaProgress>,
  val eventSink: (HomeUiEvent) -> Unit,
) : CircuitUiState

@Stable
data class UiShelf<EntityT : ShelfEntity>(
  val id: String,
  val label: String,
  val total: Int,
  val entities: LoadState<List<EntityT>>,
) {

  constructor(shelf: Shelf, entities: LoadState<List<EntityT>>) : this(
    id = shelf.id,
    label = shelf.label,
    total = shelf.total,
    entities = entities,
  )
}

sealed interface HomeUiEvent : CircuitUiEvent {
  data class OpenLibraryItem(
    val item: LibraryItem,
    val sharedTransitionKey: String,
  ) : HomeUiEvent
  data class OpenSeries(val series: Series) : HomeUiEvent
  data class OpenAuthor(val author: Author) : HomeUiEvent
}
