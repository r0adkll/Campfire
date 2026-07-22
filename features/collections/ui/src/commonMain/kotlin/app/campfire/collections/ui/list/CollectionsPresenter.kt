package app.campfire.collections.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.collections.api.CollectionsRepository
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.common.screens.CollectionsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Collection
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(CollectionsScreen::class, UserScope::class)
@Inject
class CollectionsPresenter(
  @Assisted private val navigator: Navigator,
  private val repository: CollectionsRepository,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<CollectionsUiState> {

  @Suppress("UNCHECKED_CAST")
  @Composable
  override fun present(): CollectionsUiState {
    val collectionContentState by remember {
      repository.observeAllCollections()
        .map { LoadState.Loaded(it) as LoadState<List<Collection>> }
        .catch<LoadState<out List<Collection>>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    val displayState by remember {
      settings.observeCollectionsDisplayState()
    }.collectAsState()

    return CollectionsUiState(
      collectionContentState = collectionContentState,
      displayState = displayState,
    ) { event ->
      when (event) {
        CollectionsUiEvent.Back -> navigator.pop()

        CollectionsUiEvent.ToggleDisplayState -> {
          analytics.send(ActionEvent("collections_display_state", "toggle"))
          settings.collectionsDisplayState = displayState.next()
        }

        is CollectionsUiEvent.CollectionClick -> {
          analytics.send(ContentSelected(ContentType.Collection))
          navigator.goTo(
            CollectionDetailScreen(
              event.collection.id,
              event.collection.name,
            ),
          )
        }
      }
    }
  }
}

class CollectionsObservationError(cause: Throwable) : Exception(cause)
