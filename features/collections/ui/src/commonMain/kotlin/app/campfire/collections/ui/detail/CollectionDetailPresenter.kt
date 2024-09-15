package app.campfire.collections.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.collections.api.CollectionsRepository
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(CollectionDetailScreen::class, UserScope::class)
@Inject
class CollectionDetailPresenter(
  @Assisted private val screen: CollectionDetailScreen,
  @Assisted private val navigator: Navigator,
  private val collectionsRepository: CollectionsRepository,
) : Presenter<CollectionDetailUiState> {

  @Composable
  override fun present(): CollectionDetailUiState {
    val collectionContentState by remember {
      collectionsRepository.observeCollectionItems(screen.collectionId)
        .map { CollectionContentState.Loaded(it) }
        .catch { CollectionContentState.Error }
    }.collectAsState(CollectionContentState.Loading)

    return CollectionDetailUiState(
      collectionContentState = collectionContentState,
    ) { event ->
      when (event) {
        CollectionDetailUiEvent.Back -> navigator.pop()
        is CollectionDetailUiEvent.LibraryItemClick -> navigator.goTo(LibraryItemScreen(event.libraryItem.id))
      }
    }
  }
}
