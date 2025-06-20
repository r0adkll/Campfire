package app.campfire.collections.ui.dialog

import androidx.compose.runtime.Stable
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Collection
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class AddToCollectionViewState(
  val collections: LoadState<out List<Collection>>,

  val eventSink: (AddToCollectionViewEvent) -> Unit,
) : CircuitUiState

sealed interface AddToCollectionViewEvent {
  data class CollectionClicked(val collection: Collection) : AddToCollectionViewEvent
  data class CreateCollection(val collectionName: String) : AddToCollectionViewEvent
}
