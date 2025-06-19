package app.campfire.collections.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.collections.api.CollectionsRepository
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.common.screens.CollectionsScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(CollectionsScreen::class, UserScope::class)
@Inject
class CollectionsPresenter(
  @Assisted private val navigator: Navigator,
  private val repository: CollectionsRepository,
) : Presenter<CollectionsUiState> {

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
