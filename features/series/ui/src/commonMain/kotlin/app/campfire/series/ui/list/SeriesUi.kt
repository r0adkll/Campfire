package app.campfire.series.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.layout.LargeAdaptiveColumnSize
import app.campfire.common.compose.layout.LazyCampfireGrid
import app.campfire.common.compose.widgets.ContentPagingScaffold
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.FilterBar
import app.campfire.common.compose.widgets.ItemCollectionCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.SeriesScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Series
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SeriesSortModes
import app.campfire.core.settings.SortDirection
import app.campfire.filters.ContentFilterResult
import app.campfire.filters.ContentFilterUi
import app.campfire.filters.SortModeUi
import app.campfire.ui.appbar.CampfireAppBar
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.empty_series_items_message
import campfire.features.series.ui.generated.resources.error_series_items_message
import campfire.features.series.ui.generated.resources.filter_bar_series_count
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@CircuitInject(SeriesScreen::class, UserScope::class)
@Composable
fun Series(
  state: SeriesUiState,
  campfireAppBar: CampfireAppBar,
  contentFilterUi: ContentFilterUi,
  sortModeUi: SortModeUi,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()

  val appBarBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  AttachScrollBehaviorToLocalNavigationBar(appBarBehavior)

  val overlayHost = LocalOverlayHost.current
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
    contentWindowInsets = CampfireWindowInsets.exclude(WindowInsets.navigationBars),
  ) { paddingValues ->
    when (state.seriesContentState) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_series_items_message),
        modifier = Modifier.padding(paddingValues),
      )

      is LoadState.Loaded -> LoadedState(
        totalCount = state.totalCount,
        filter = state.filter,
        sortMode = state.sortMode,
        sortDirection = state.sortDirection,
        items = state.seriesContentState.data,
        onSeriesClick = { state.eventSink(SeriesUiEvent.SeriesClicked(it)) },
        onFilterClick = {
          scope.launch {
            val result = contentFilterUi.showContentFilterBottomSheet(
              overlayHost = overlayHost,
              current = state.filter,
              allowedCategories = contentFilterUi.seriesFilterCategories,
            )
            if (result is ContentFilterResult.Selected) {
              state.eventSink(SeriesUiEvent.FilterChanged(result.filter))
            }
          }
        },
        onSortClick = {
          scope.launch {
            val updatedSortMode = sortModeUi.showContentSortModeBottomSheet(
              overlayHost = overlayHost,
              current = state.sortMode,
              currentDirection = state.sortDirection,
              config = SeriesSortModes,
            )
            if (updatedSortMode != null) {
              state.eventSink(SeriesUiEvent.SortModeChanged(updatedSortMode))
            }
          }
        },
        contentPadding = paddingValues,
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoadedState(
  totalCount: Int,
  filter: ContentFilter?,
  sortMode: ContentSortMode,
  sortDirection: SortDirection,
  items: Flow<PagingData<Series>>,
  onSeriesClick: (Series) -> Unit,
  onFilterClick: () -> Unit,
  onSortClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyGridState = rememberLazyGridState(),
) {
  val lazyPagingItems = items.collectAsLazyPagingItems()
  ContentPagingScaffold(
    modifier = modifier,
    lazyPagingItems = lazyPagingItems,
    emptyMessage = stringResource(Res.string.empty_series_items_message),
    indicatorPadding = contentPadding.calculateTopPadding(),
  ) {
    LazyCampfireGrid(
      columns = GridCells.Adaptive(LargeAdaptiveColumnSize),
      state = state,
      contentPadding = contentPadding + PaddingValues(horizontal = ContentPadding),
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item(
        span = { GridItemSpan(this.maxLineSpan) },
        key = "filter-bar",
      ) {
        FilterBar(
          count = {
            if (totalCount != INVALID_SERIES_COUNT) {
              Text(
                text = pluralStringResource(Res.plurals.filter_bar_series_count, totalCount, totalCount),
              )
            } else {
              Text("--")
            }
          },
          itemDisplayState = ItemDisplayState.Grid,
          isFiltered = filter != null,
          onFilterClick = onFilterClick,
          sortMode = sortMode,
          sortDirection = sortDirection,
          onSortClick = onSortClick,
        )
      }

      items(
        count = lazyPagingItems.itemCount,
        key = lazyPagingItems.itemKey { it.id },
        contentType = lazyPagingItems.itemContentType { "series-item" },
      ) { index ->
        val series = lazyPagingItems[index]
        if (series == null) {
          PlaceholderItem()
        } else {
          ItemCollectionCard(
            sharedTransitionKey = series.id,
            name = series.name,
            description = series.description,
            items = series.books ?: emptyList(),
            onClick = { onSeriesClick(series) },
            modifier = Modifier
              .fillMaxWidth()
              .animateItem(),
          )
        }
      }

      appendingIndicatorItem()
    }
  }
}

private val ContentPadding = 16.dp
