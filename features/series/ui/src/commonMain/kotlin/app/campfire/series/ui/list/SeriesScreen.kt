package app.campfire.series.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.SeriesCard
import app.campfire.common.screens.SeriesScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.Series
import app.campfire.ui.appbar.CampfireAppBar
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.error_series_items_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import kotlinx.datetime.format.Padding
import org.jetbrains.compose.resources.stringResource

@CircuitInject(SeriesScreen::class, UserScope::class)
@Composable
fun Series(
  state: SeriesUiState,
  campfireAppBar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val appBarBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    topBar = {
      // Injected appbar that injects its own presenter to consistently load its state
      // across multiple services.
      campfireAppBar(
        { },
        Modifier,
        appBarBehavior,
      )
    },
    modifier = modifier.nestedScroll(appBarBehavior.nestedScrollConnection),
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
  state: LazyListState = rememberLazyListState(),
) {
  LazyColumn(
    state = state,
    contentPadding = contentPadding + PaddingValues(
      horizontal = 16.dp
    ),
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    items(items) { series ->
      SeriesCard(
        series = series,
        modifier = Modifier
          .fillMaxWidth()
          .animateItemPlacement()
          .clickable { onSeriesClick(series) },
      )
    }
  }
}
