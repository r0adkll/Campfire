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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.FilterBar
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LibraryListItem
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.LibraryScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.settings.ItemDisplayState
import app.campfire.ui.appbar.CampfireAppBar
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.empty_library_items_message
import campfire.features.libraries.ui.generated.resources.error_library_items_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(LibraryScreen::class, UserScope::class)
@Composable
fun Library(
  state: LibraryUiState,
  campfireAppBar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val appBarBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    topBar = {
      // Injected appbar that injects its own presenter to consistently load its state
      // across multiple services.
      campfireAppBar(
        { state.eventSink(LibraryUiEvent.OpenSearch) },
        Modifier,
        appBarBehavior,
      )
    },
    modifier = modifier.nestedScroll(appBarBehavior.nestedScrollConnection),
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
        onFilterClick = { state.eventSink(LibraryUiEvent.ToggleFilter) },
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
  modifier: Modifier = Modifier,
  columns: Int = 3,
  contentPadding: PaddingValues = PaddingValues(),
) {
  when (itemDisplayState) {
    ItemDisplayState.List -> LibraryList(
      items = items,
      onItemClick = onItemClick,
      itemDisplayState = itemDisplayState,
      onDisplayStateClick = onDisplayStateClick,
      onFilterClick = onFilterClick,
      modifier = modifier,
      contentPadding = contentPadding,
    )
    ItemDisplayState.Grid -> LibraryGrid(
      items = items,
      onItemClick = onItemClick,
      itemDisplayState = itemDisplayState,
      onDisplayStateClick = onDisplayStateClick,
      onFilterClick = onFilterClick,
      modifier = modifier,
      columns = columns,
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
  modifier: Modifier = Modifier,
  columns: Int = 3,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyGridState = rememberLazyGridState(),
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(columns),
    state = state,
    modifier = modifier,
    contentPadding = contentPadding,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(
      span = { GridItemSpan(columns) },
    ) {
      FilterBar(
        itemCount = items.size,
        itemDisplayState = itemDisplayState,
        onDisplayStateClick = onDisplayStateClick,
        isFiltered = false,
        onFilterClick = onFilterClick,
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
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyListState = rememberLazyListState(),
) {
  LazyColumn(
    state = state,
    contentPadding = contentPadding,
    modifier = modifier,
  ) {
    stickyHeader {
      FilterBar(
        itemCount = items.size,
        itemDisplayState = itemDisplayState,
        onDisplayStateClick = onDisplayStateClick,
        isFiltered = false,
        onFilterClick = onFilterClick,
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
