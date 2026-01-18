package app.campfire.collections.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.collections.api.CollectionsRepository
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.common.screens.CollectionsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Collection
import app.campfire.crashreporting.CrashReporter
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
  private val analytics: Analytics,
) : NonPausablePresenter<CollectionsUiState> {

  @Suppress("UNCHECKED_CAST")
  @Composable
  override fun present(): CollectionsUiState {
    val collectionContentState by remember {
      repository.observeAllCollections()
        .map { LoadState.Loaded(it) as LoadState<List<Collection>> }
        .catch { e ->
          CrashReporter.record(CollectionsObservationError(e))
          emit(LoadState.Error as LoadState<List<Collection>>)
        }
    }.collectAsState(LoadState.Loading)

    return CollectionsUiState(
      collectionContentState = collectionContentState,
    ) { event ->
      when (event) {
        CollectionsUiEvent.Back -> navigator.pop()

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
