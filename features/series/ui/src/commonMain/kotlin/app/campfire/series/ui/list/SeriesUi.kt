package app.campfire.series.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.layout.LargeAdaptiveColumnSize
import app.campfire.common.compose.layout.LazyCampfireGrid
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.ItemCollectionCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.SeriesScreen
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.Series
import app.campfire.ui.appbar.CampfireAppBar
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.empty_series_items_message
import campfire.features.series.ui.generated.resources.error_series_items_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(SeriesScreen::class, UserScope::class)
@Composable
fun Series(
  state: SeriesUiState,
  campfireAppBar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)
  val appBarBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    topBar = {
      if (!windowSizeClass.isSupportingPaneEnabled) {
        // Injected appbar that injects its own presenter to consistently load its state
        // across multiple services.
        campfireAppBar(
          Modifier,
          appBarBehavior,
        )
      }
    },
    modifier = modifier.fluentIf(!windowSizeClass.isSupportingPaneEnabled) {
      nestedScroll(appBarBehavior.nestedScrollConnection)
    },
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    when (state.seriesContentState) {
      SeriesContentState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      SeriesContentState.Error -> ErrorListState(
        message = stringResource(Res.string.error_series_items_message),
        modifier = Modifier.padding(paddingValues),
      )

      is SeriesContentState.Loaded -> LoadedState(
        items = state.seriesContentState.series,
        onSeriesClick = { state.eventSink(SeriesUiEvent.SeriesClicked(it)) },
        contentPadding = paddingValues,
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoadedState(
  items: List<Series>,
  onSeriesClick: (Series) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyGridState = rememberLazyGridState(),
) {
  Box(
    modifier = modifier.fillMaxSize(),
  ) {
    LazyCampfireGrid(
      columns = GridCells.Adaptive(LargeAdaptiveColumnSize),
      state = state,
      contentPadding = contentPadding + PaddingValues(ContentPadding),
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(
        items = items,
        key = { it.id },
      ) { series ->
        ItemCollectionCard(
          name = series.name,
          description = series.description,
          items = series.books ?: emptyList(),
          modifier = Modifier
            .fillMaxWidth()
            .animateItem()
            .clickable { onSeriesClick(series) },
        )
      }
    }

    if (items.isEmpty()) {
      EmptyState(
        message = stringResource(Res.string.empty_series_items_message),
      )
    }
  }
}

private val ContentPadding = 16.dp
