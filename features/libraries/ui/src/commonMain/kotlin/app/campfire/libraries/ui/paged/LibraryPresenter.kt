package app.campfire.libraries.ui.paged

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.core.di.UserScope
import app.campfire.core.settings.ItemDisplayState
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.settings.api.CampfireSettings
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

internal const val INVALID_ITEM_COUNT = -1

@OptIn(ExperimentalCoroutinesApi::class)
@CircuitInject(LibraryScreen::class, UserScope::class)
@Inject
class LibraryPresenter(
  @Assisted private val screen: LibraryScreen,
  @Assisted private val navigator: Navigator,
  private val userRepository: UserRepository,
  private val repository: LibraryRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<LibraryUiState> {

  @Composable
  override fun present(): LibraryUiState {
    // Using a pager requires us to remember the coroutine scope passed the
    // composition of this pager / ui. We should remember it until this screen
    // leaves the back stack
    val scope = rememberRetainedCoroutineScope()

    var itemFilter by rememberRetainedSaveable {
      mutableStateOf(screen.filter)
    }

    val sortMode by remember {
      settings.observeLibrarySortMode()
    }.collectAsState(settings.librarySortMode)

    val sortDirection by remember {
      settings.observeLibrarySortDirection()
    }.collectAsState(settings.librarySortDirection)

    val currentUser by remember {
      userRepository.observeStatefulCurrentUser()
    }.collectAsState()

    val lazyPagingItems = rememberRetained(currentUser, sortMode, sortDirection, itemFilter) {
      repository.createLibraryItemPager(
        user = currentUser,
        filter = itemFilter,
        sortMode = sortMode,
        sortDirection = sortDirection,
      ).flow.cachedIn(scope)
    }.collectAsLazyPagingItems()

    val totalItemCount by rememberRetained(sortMode, sortDirection, itemFilter) {
      repository.observeFilteredLibraryCount(
        filter = itemFilter,
        sortMode = sortMode,
        sortDirection = sortDirection,
      ).map { it ?: INVALID_ITEM_COUNT }
    }.collectAsState(INVALID_ITEM_COUNT)

    val itemDisplayState by settings.observeLibraryItemDisplayState()
      .collectAsState(ItemDisplayState.List)

    val offlineDownloads by remember {
      offlineDownloadManager.observeAll()
        .map { downloads ->
          downloads.associateBy { it.libraryItemId }
        }
    }.collectAsState(emptyMap())

    return LibraryUiState(
      lazyPagingItems = lazyPagingItems,
      totalItemCount = totalItemCount,
      sort = LibrarySort(sortMode, sortDirection),
      filter = itemFilter,
      offlineStates = offlineDownloads,
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
            settings.librarySortDirection = sortDirection.flip()
          }
          settings.librarySortMode = event.mode
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
