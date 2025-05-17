package app.campfire.collections.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.collections.api.CollectionsRepository
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.common.screens.CollectionsScreen
import app.campfire.core.di.UserScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Inject
class CollectionsPresenter(
  @Assisted private val navigator: Navigator,
  private val repository: CollectionsRepository,
) : Presenter<CollectionsUiState> {

  @CircuitInject(CollectionsScreen::class, UserScope::class)
  @AssistedFactory
  fun interface Factory {
    fun create(navigator: Navigator): CollectionsPresenter
  }

  @Composable
  override fun present(): CollectionsUiState {
    val collectionContentState by remember {
      repository.observeAllCollections()
        .map { CollectionContentState.Loaded(it) }
        .catch { CollectionContentState.Error }
    }.collectAsState(CollectionContentState.Loading)

    return CollectionsUiState(
      collectionContentState = collectionContentState,
    ) { event ->
      when (event) {
        is CollectionsUiEvent.CollectionClick -> navigator.goTo(
          CollectionDetailScreen(
            event.collection.id,
            event.collection.name,
          ),
        )
      }
    }
  }
}
