package app.campfire.libraries.ui.paged

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.settings.ItemDisplayState
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.filtering.FilteringRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@CircuitInject(LibraryScreen::class, UserScope::class)
@Inject
class LibraryPresenter(
  @Assisted private val screen: LibraryScreen,
  @Assisted private val navigator: Navigator,
  private val repository: LibraryRepository,
  private val filteringRepository: FilteringRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<LibraryUiState> {

  @Composable
  override fun present(): LibraryUiState {
    val scope = rememberCoroutineScope()

    var itemFilter by rememberRetainedSaveable {
      mutableStateOf(screen.filter)
    }

    val sortMode by remember {
      settings.observeSortMode()
    }.collectAsState(settings.sortMode)

    val sortDirection by remember {
      settings.observeSortDirection()
    }.collectAsState(settings.sortDirection)

    val contentState by remember(sortMode, sortDirection, itemFilter) {
      repository.observeLibraryItemPager(
        filter = itemFilter,
        sortMode = sortMode,
        sortDirection = sortDirection,
      )
        .map {
          LoadState.Loaded(it.flow.cachedIn(scope))
        }
        .catch<LoadState<out Flow<PagingData<LibraryItem>>>> {
          emit(LoadState.Error)
        }
    }.collectAsState(LoadState.Loading)

    val itemDisplayState by settings.observeLibraryItemDisplayState()
      .collectAsState(ItemDisplayState.List)

    val totalItemCount by remember {
      filteringRepository.observeFilterData()
        .map { it.bookCount }
        .catch { emit(-2) }
    }.collectAsState(-1)

//    val offlineDownloads by remember {
//      snapshotFlow { contentState.dataOrNull }
//        .filterNotNull()
//        .flatMapLatest { items ->
//          offlineDownloadManager.observeForItems(items)
//        }
//    }.collectAsState(emptyMap())

    return LibraryUiState(
      contentState = contentState,
      totalItemCount = totalItemCount,
      sort = LibrarySort(sortMode, sortDirection),
      filter = itemFilter,
      offlineStates = emptyMap(),
      itemDisplayState = itemDisplayState,
    ) { event ->
      when (event) {
        LibraryUiEvent.ToggleItemDisplayState -> {
          settings.libraryItemDisplayState = when (itemDisplayState) {
            ItemDisplayState.List -> ItemDisplayState.Grid
            ItemDisplayState.Grid -> ItemDisplayState.GridDense
            ItemDisplayState.GridDense -> ItemDisplayState.List
          }.also {
            analytics.send(ActionEvent("item_display", "toggled", it.storageKey))
          }
        }

        is LibraryUiEvent.SortModeSelected -> {
          analytics.send(ActionEvent("sort_mode", "selected", event.mode.storageKey))
          if (sortMode == event.mode) {
            settings.sortDirection = sortDirection.flip()
          }
          settings.sortMode = event.mode
        }

        is LibraryUiEvent.ItemFilterSelected -> {
          analytics.send(ActionEvent("item_filter", "selected"))
          itemFilter = event.filter
        }

        is LibraryUiEvent.ItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.libraryItem.id))
        }
      }
    }
  }
}
