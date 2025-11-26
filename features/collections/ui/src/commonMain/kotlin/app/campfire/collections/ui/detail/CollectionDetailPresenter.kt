package app.campfire.collections.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.collections.api.CollectionsRepository
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(CollectionDetailScreen::class, UserScope::class)
@Inject
class CollectionDetailPresenter(
  @Assisted private val screen: CollectionDetailScreen,
  @Assisted private val navigator: Navigator,
  private val collectionsRepository: CollectionsRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val analytics: Analytics,
) : Presenter<CollectionDetailUiState> {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): CollectionDetailUiState {
    val scope = rememberCoroutineScope()

    val collection by remember {
      collectionsRepository.observeCollection(screen.collectionId)
    }.collectAsState(null)

    val collectionContentState by remember {
      collectionsRepository.observeCollectionItems(screen.collectionId)
        .map { LoadState.Loaded(it) }
        .catch<LoadState<out List<LibraryItem>>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    val offlineDownloads by remember {
      snapshotFlow { collectionContentState.dataOrNull }
        .filterNotNull()
        .flatMapLatest { items ->
          offlineDownloadManager.observeForItems(items)
        }
    }.collectAsState(emptyMap())

    return CollectionDetailUiState(
      collection = collection,
      collectionContentState = collectionContentState,
      offlineStates = offlineDownloads,
    ) { event ->
      when (event) {
        CollectionDetailUiEvent.Back -> navigator.pop()
        CollectionDetailUiEvent.Delete -> scope.launch {
          analytics.send(ActionEvent("collection", "deleted"))
          collectionsRepository.deleteCollection(screen.collectionId)
          navigator.pop()
        }

        is CollectionDetailUiEvent.DeleteItems -> scope.launch {
          analytics.send(ActionEvent("collection_item", "deleted"))
          collectionsRepository.removeFromCollection(
            bookIds = event.items.map { it.id },
            collectionId = screen.collectionId,
          )
        }

        is CollectionDetailUiEvent.LibraryItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(
            LibraryItemScreen(
              libraryItemId = event.libraryItem.id,
              sharedTransitionKey = event.libraryItem.id + screen.collectionName,
            ),
          )
        }
      }
    }
  }
}
