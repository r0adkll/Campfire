package app.campfire.libraries.ui.paged

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
//import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.layout.DefaultAdaptiveColumnSize
import app.campfire.common.compose.layout.DenseAdaptiveColumnSize
import app.campfire.common.compose.layout.LazyCampfireGrid
import app.campfire.common.compose.widgets.CampfireLoadingIndicator
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.FilterBar
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LibraryListItem
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.OfflineStatusIndicator
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.offline.OfflineStatus
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import app.campfire.libraries.api.LibraryItemFilter
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.libraries.ui.paged.sheets.filters.LibraryItemFilterResult
import app.campfire.libraries.ui.paged.sheets.filters.showItemFilterOverlay
import app.campfire.libraries.ui.paged.sheets.sort.SortModeResult
import app.campfire.libraries.ui.paged.sheets.sort.showSortModeBottomSheet
import app.campfire.ui.appbar.CampfireAppBar
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.empty_library_items_message
import campfire.features.libraries.ui.generated.resources.error_library_items_message
import campfire.features.libraries.ui.generated.resources.filter_bar_book_count
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@CircuitInject(LibraryScreen::class, UserScope::class)
@Composable
fun LibraryUi(
  state: LibraryUiState,
  campfireAppBar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  val overlayHost by rememberUpdatedState(LocalOverlayHost.current)

  val appBarBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  AttachScrollBehaviorToLocalNavigationBar(appBarBehavior)

  Scaffold(
    topBar = {
      // Injected appbar that injects its own presenter to consistently load its state
      // across multiple services.
      campfireAppBar(
        Modifier,
        appBarBehavior,
      )
    },
    modifier = modifier.nestedScroll(appBarBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    when (state.contentState) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_library_items_message),
        modifier = Modifier.padding(paddingValues),
      )

      is LoadState.Loaded -> LoadedContent(
        totalCount = state.totalItemCount,
        pagingDataFlow = state.contentState.data,
        offlineStates = state.offlineStates,
        onItemClick = { state.eventSink(LibraryUiEvent.ItemClick(it)) },
        itemDisplayState = state.itemDisplayState,
        onDisplayStateClick = { state.eventSink(LibraryUiEvent.ToggleItemDisplayState) },
        filter = state.filter,
        onFilterClick = {
          coroutineScope.launch {
            val result = overlayHost.showItemFilterOverlay(
              filter = state.filter,
            )

            if (result is LibraryItemFilterResult.Selected) {
              state.eventSink(LibraryUiEvent.ItemFilterSelected(result.filter))
            }
          }
        },
        sortMode = state.sort.mode,
        sortDirection = state.sort.direction,
        onSortClick = {
          coroutineScope.launch {
            val result = overlayHost.showSortModeBottomSheet(
              currentMode = state.sort.mode,
              currentDirection = state.sort.direction,
            )
            if (result is SortModeResult.Selected) {
              state.eventSink(LibraryUiEvent.SortModeSelected(result.mode))
            }
          }
        },
        contentPadding = paddingValues,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadedContent(
  totalCount: Int,
  pagingDataFlow: Flow<PagingData<LibraryItem>>,
  offlineStates: Map<LibraryItemId, OfflineDownload>,
  onItemClick: (LibraryItem) -> Unit,
  itemDisplayState: ItemDisplayState,
  onDisplayStateClick: () -> Unit,
  filter: LibraryItemFilter?,
  onFilterClick: () -> Unit,
  sortMode: SortMode,
  sortDirection: SortDirection,
  onSortClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  val lazyPagingItems = pagingDataFlow.collectAsLazyPagingItems()
  val isRefreshing = lazyPagingItems.loadState.refresh == androidx.paging.LoadState.Loading
  val ptrState = rememberPullToRefreshState()
  PullToRefreshBox(
    state = ptrState,
    isRefreshing = isRefreshing,
    onRefresh = { lazyPagingItems.refresh() },
    modifier = modifier,
    indicator = {
      CampfireLoadingIndicator(
        state = ptrState,
        isRefreshing = isRefreshing,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = contentPadding.calculateTopPadding())
      )
    }
  ) {
    when (itemDisplayState) {
      ItemDisplayState.List -> LibraryList(
        totalCount = totalCount,
        items = lazyPagingItems,
        offlineStates = offlineStates,
        onItemClick = onItemClick,
        itemDisplayState = itemDisplayState,
        onDisplayStateClick = onDisplayStateClick,
        filter = filter,
        onFilterClick = onFilterClick,
        sortMode = sortMode,
        sortDirection = sortDirection,
        onSortClick = onSortClick,
//        modifier = modifier,
        contentPadding = contentPadding,
      )

      ItemDisplayState.GridDense,
      ItemDisplayState.Grid,
        -> LibraryGrid(
        totalCount = totalCount,
        items = lazyPagingItems,
        offlineStates = offlineStates,
        onItemClick = onItemClick,
        itemDisplayState = itemDisplayState,
        onDisplayStateClick = onDisplayStateClick,
        filter = filter,
        onFilterClick = onFilterClick,
        sortMode = sortMode,
        sortDirection = sortDirection,
        onSortClick = onSortClick,
//        modifier = modifier,
        contentPadding = contentPadding + PaddingValues(
          horizontal = 16.dp,
        ),
      )
    }
  }



  if (lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.isIdle) {
    EmptyState(
      message = stringResource(Res.string.empty_library_items_message),
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryGrid(
  totalCount: Int,
  items: LazyPagingItems<LibraryItem>,
  offlineStates: Map<LibraryItemId, OfflineDownload>,
  onItemClick: (LibraryItem) -> Unit,
  itemDisplayState: ItemDisplayState,
  onDisplayStateClick: () -> Unit,
  filter: LibraryItemFilter?,
  onFilterClick: () -> Unit,
  sortMode: SortMode,
  sortDirection: SortDirection,
  onSortClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyGridState = rememberLazyGridState(),
) {
  LazyCampfireGrid(
    state = state,
    modifier = modifier,
    contentPadding = contentPadding,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    columns = when (itemDisplayState) {
      ItemDisplayState.GridDense -> GridCells.Adaptive(DenseAdaptiveColumnSize)
      else -> GridCells.Adaptive(DefaultAdaptiveColumnSize)
    },
  ) {
    item(
      span = { GridItemSpan(this.maxLineSpan) },
      key = "filter-bar",
    ) {
      FilterBar(
        count = {
          if (totalCount != INVALID_ITEM_COUNT) {
            Text(
              text = pluralStringResource(Res.plurals.filter_bar_book_count, totalCount, totalCount)
            )
          } else {
            Text("--")
          }
        },
        itemDisplayState = itemDisplayState,
        onDisplayStateClick = onDisplayStateClick,
        isFiltered = filter != null,
        onFilterClick = onFilterClick,
        sortMode = sortMode,
        sortDirection = sortDirection,
        onSortClick = onSortClick,
      )
    }


    items(
      count = items.itemCount,
      key = items.itemKey { it.id },
      contentType = items.itemContentType { "library-item" },
    ) { index ->
      val item = items[index]
      if (item == null) {
        // TODO: Placeholder
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text("Placeholder for $index")
        }
      } else {
        val offlineStatus = offlineStates[item.id]
        LibraryItemCard(
          item = item,
          offlineStatus = offlineStatus.asWidgetStatus(),
          onClick = { onItemClick(item) },
          modifier = Modifier.animateItem(),
          showInformation = itemDisplayState != ItemDisplayState.GridDense,
          shape = when (itemDisplayState) {
            ItemDisplayState.GridDense -> MaterialTheme.shapes.medium
            else -> MaterialTheme.shapes.largeIncreased
          },
        )
      }
    }

    if (items.loadState.append is androidx.paging.LoadState.Loading) {
      item(
        span = { GridItemSpan(maxLineSpan) }
      ) {
        CircularProgressIndicator(
          modifier =
            Modifier
              .fillMaxWidth()
              .wrapContentWidth(Alignment.CenterHorizontally)
        )
      }
    }
  }
}

@Composable
fun LibraryList(
  totalCount: Int,
  items: LazyPagingItems<LibraryItem>,
  offlineStates: Map<LibraryItemId, OfflineDownload>,
  onItemClick: (LibraryItem) -> Unit,
  itemDisplayState: ItemDisplayState,
  onDisplayStateClick: () -> Unit,
  filter: LibraryItemFilter?,
  onFilterClick: () -> Unit,
  sortMode: SortMode,
  sortDirection: SortDirection,
  onSortClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyListState = rememberLazyListState(),
) {
  LazyColumn(
    state = state,
    contentPadding = contentPadding,
    modifier = modifier.fillMaxSize(),
  ) {
    stickyHeader(key = "filter-bar") {
      FilterBar(
        count = {
          if (totalCount != INVALID_ITEM_COUNT) {
            Text(
              text = pluralStringResource(Res.plurals.filter_bar_book_count, totalCount, totalCount)
            )
          } else {
            Text("--")
          }
        },
        itemDisplayState = itemDisplayState,
        onDisplayStateClick = onDisplayStateClick,
        isFiltered = filter != null,
        onFilterClick = onFilterClick,
        sortMode = sortMode,
        sortDirection = sortDirection,
        onSortClick = onSortClick,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
      )
    }

    items(
      count = items.itemCount,
      key = items.itemKey { it.id },
      contentType = items.itemContentType { "library-item-list" },
    ) { index ->
      val item = items[index]
      if (item == null) {
        // TODO: Placeholder
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text("Placeholder for $index")
        }
      } else {
        val offlineStatus = offlineStates[item.id].asWidgetStatus()
        LibraryListItem(
          item = item,
          modifier = Modifier
            .animateItem()
            .clickable { onItemClick(item) },
          trailingContent = {
            if (offlineStatus != OfflineStatus.None) {
              OfflineStatusRow(
                status = offlineStatus,
              )
            }
          },
        )
      }
    }

    if (items.loadState.append is androidx.paging.LoadState.Loading) {
      item {
        CircularProgressIndicator(
          modifier =
            Modifier
              .fillMaxWidth()
              .wrapContentWidth(Alignment.CenterHorizontally)
        )
      }
    }
  }
}

@Composable
private fun OfflineStatusRow(
  status: OfflineStatus,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.padding(ButtonDefaults.TextButtonWithIconContentPadding),
    horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = when (status) {
        OfflineStatus.Available -> "Available"
        is OfflineStatus.Downloading -> "Downloading"
        OfflineStatus.Failed -> "Failed"
        OfflineStatus.Queued -> "Queued"
        OfflineStatus.None -> ""
      },
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
    )

    OfflineStatusIndicator(
      status = status,
      tint = MaterialTheme.colorScheme.onSurface,
    )
  }
}
