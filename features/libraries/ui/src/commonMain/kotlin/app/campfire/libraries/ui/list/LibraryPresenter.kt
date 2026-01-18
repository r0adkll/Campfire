package app.campfire.libraries.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.settings.ItemDisplayState
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
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
  private val offlineDownloadManager: OfflineDownloadManager,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<LibraryUiState> {

  @Composable
  override fun present(): LibraryUiState {
    // TODO: We should store this in user preferences so it persists
    //  between UI and process changes
    var itemFilter by remember { mutableStateOf(screen.filter) }

    val sortMode by remember {
      settings.observeSortMode()
    }.collectAsState(settings.sortMode)

    val sortDirection by remember {
      settings.observeSortDirection()
    }.collectAsState(settings.sortDirection)

    val contentState by remember(sortMode, sortDirection, itemFilter) {
      repository.observeLibraryItems(
        filter = itemFilter,
        sortMode = sortMode,
        sortDirection = sortDirection,
      )
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    val itemDisplayState by settings.observeLibraryItemDisplayState()
      .collectAsState(ItemDisplayState.List)

    val offlineDownloads by remember {
      snapshotFlow { contentState.dataOrNull }
        .filterNotNull()
        .flatMapLatest { items ->
          offlineDownloadManager.observeForItems(items)
        }
    }.collectAsState(emptyMap())

    return LibraryUiState(
      contentState = contentState,
      sort = LibrarySort(sortMode, sortDirection),
      filter = itemFilter,
      offlineStates = offlineDownloads,
      itemDisplayState = itemDisplayState,
    ) { event ->
      when (event) {
        LibraryUiEvent.ToggleItemDisplayState -> {
          settings.libraryItemDisplayState = when (itemDisplayState) {
            ItemDisplayState.List -> ItemDisplayState.Grid
            ItemDisplayState.Grid -> ItemDisplayState.List
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
