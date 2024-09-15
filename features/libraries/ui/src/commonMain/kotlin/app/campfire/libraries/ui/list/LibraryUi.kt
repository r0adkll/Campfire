package app.campfire.libraries.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.layout.LazyCampfireGrid
import app.campfire.common.compose.layout.LazyContentSize
import app.campfire.common.compose.layout.LocalSupportingContentState
import app.campfire.common.compose.layout.SupportingContentState
import app.campfire.common.compose.layout.contentWindowInsets
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.FilterBar
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LibraryListItem
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.LibraryScreen
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.LibraryItem
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import app.campfire.libraries.ui.sort.SortModeResult
import app.campfire.libraries.ui.sort.showSortModeBottomSheet
import app.campfire.ui.appbar.CampfireAppBar
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.empty_library_items_message
import campfire.features.libraries.ui.generated.resources.error_library_items_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@CircuitInject(LibraryScreen::class, UserScope::class)
@Composable
fun Library(
  state: LibraryUiState,
  campfireAppBar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  val overlayHost by rememberUpdatedState(LocalOverlayHost.current)
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)

  val appBarBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    topBar = {
      if (!windowSizeClass.isSupportingPaneEnabled) {
        // Injected appbar that injects its own presenter to consistently load its state
        // across multiple services.
        campfireAppBar(
          { state.eventSink(LibraryUiEvent.OpenSearch) },
          Modifier,
          appBarBehavior,
        )
      }
    },
    modifier = modifier.fluentIf(!windowSizeClass.isSupportingPaneEnabled) {
      nestedScroll(appBarBehavior.nestedScrollConnection)
    },
    contentWindowInsets = windowSizeClass.contentWindowInsets,
  ) { paddingValues ->
    when (state.contentState) {
      LibraryContentState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LibraryContentState.Error -> ErrorListState(
        message = stringResource(Res.string.error_library_items_message),
        modifier = Modifier.padding(paddingValues),
      )

      is LibraryContentState.Loaded -> LoadedContent(
        items = state.contentState.items,
        onItemClick = { state.eventSink(LibraryUiEvent.ItemClick(it)) },
        itemDisplayState = state.itemDisplayState,
        onDisplayStateClick = { state.eventSink(LibraryUiEvent.ToggleItemDisplayState) },
        onFilterClick = { state.eventSink(LibraryUiEvent.FilterClick) },
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

@Composable
private fun LoadedContent(
  items: List<LibraryItem>,
  onItemClick: (LibraryItem) -> Unit,
  itemDisplayState: ItemDisplayState,
  onDisplayStateClick: () -> Unit,
  onFilterClick: () -> Unit,
  sortMode: SortMode,
  sortDirection: SortDirection,
  onSortClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  when (itemDisplayState) {
    ItemDisplayState.List -> LibraryList(
      items = items,
      onItemClick = onItemClick,
      itemDisplayState = itemDisplayState,
      onDisplayStateClick = onDisplayStateClick,
      onFilterClick = onFilterClick,
      sortMode = sortMode,
      sortDirection = sortDirection,
      onSortClick = onSortClick,
      modifier = modifier,
      contentPadding = contentPadding,
    )
    ItemDisplayState.Grid -> LibraryGrid(
      items = items,
      onItemClick = onItemClick,
      itemDisplayState = itemDisplayState,
      onDisplayStateClick = onDisplayStateClick,
      onFilterClick = onFilterClick,
      sortMode = sortMode,
      sortDirection = sortDirection,
      onSortClick = onSortClick,
      modifier = modifier,
      contentPadding = contentPadding + PaddingValues(
        horizontal = 16.dp,
      ),
    )
  }

  if (items.isEmpty()) {
    EmptyState(
      message = stringResource(Res.string.empty_library_items_message),
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGrid(
  items: List<LibraryItem>,
  onItemClick: (LibraryItem) -> Unit,
  itemDisplayState: ItemDisplayState,
  onDisplayStateClick: () -> Unit,
  onFilterClick: () -> Unit,
  sortMode: SortMode,
  sortDirection: SortDirection,
  onSortClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyGridState = rememberLazyGridState(),
) {
  LazyCampfireGrid(
    gridCells = {
      when(it) {
        LazyContentSize.Small -> GridCells.Fixed(3)
        LazyContentSize.Large -> GridCells.Fixed(5)
      }
    },
    state = state,
    modifier = modifier,
    contentPadding = contentPadding,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(
      span = { GridItemSpan(this.maxLineSpan) },
      key = "filter-bar",
    ) {
      FilterBar(
        itemCount = items.size,
        itemDisplayState = itemDisplayState,
        onDisplayStateClick = onDisplayStateClick,
        isFiltered = false,
        onFilterClick = onFilterClick,
        sortMode = sortMode,
        sortDirection = sortDirection,
        onSortClick = onSortClick,
      )
    }
    items(
      items = items,
      key = { it.id },
    ) { item ->
      LibraryItemCard(
        item = item,
        modifier = Modifier
          .animateItemPlacement()
          .clickable { onItemClick(item) },
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryList(
  items: List<LibraryItem>,
  onItemClick: (LibraryItem) -> Unit,
  itemDisplayState: ItemDisplayState,
  onDisplayStateClick: () -> Unit,
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
    modifier = modifier,
  ) {
    stickyHeader(key = "filter-bar") {
      FilterBar(
        itemCount = items.size,
        itemDisplayState = itemDisplayState,
        onDisplayStateClick = onDisplayStateClick,
        isFiltered = false,
        onFilterClick = onFilterClick,
        sortMode = sortMode,
        sortDirection = sortDirection,
        onSortClick = onSortClick,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
      )
    }
    items(
      items = items,
      key = { it.id },
    ) { item ->
      LibraryListItem(
        item = item,
        modifier = Modifier
          .animateItemPlacement()
          .clickable { onItemClick(item) },
      )
    }
  }
}
