package app.campfire.series.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.offline.OfflineStatus
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.error_series_detail_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(SeriesDetailScreen::class, UserScope::class)
@Composable
fun SeriesDetail(
  screen: SeriesDetailScreen,
  state: SeriesDetailUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(screen.seriesName) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          IconButton(
            onClick = { state.eventSink(SeriesDetailUiEvent.Back) },
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
          }
        },
      )
    },
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    when (state.seriesContentState) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_series_detail_message),
        modifier = Modifier.padding(paddingValues),
      )

      is LoadState.Loaded -> LoadedState(
        seriesName = screen.seriesName,
        items = state.seriesContentState.data,
        offlineStatus = { state.offlineStates[it].asWidgetStatus() },
        onLibraryItemClick = { state.eventSink(SeriesDetailUiEvent.LibraryItemClick(it)) },
        contentPadding = paddingValues,
      )
    }
  }
}

@Composable
private fun LoadedState(
  seriesName: String,
  items: List<LibraryItem>,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  onLibraryItemClick: (LibraryItem) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  gridState: LazyGridState = rememberLazyGridState(),
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    state = gridState,
    modifier = modifier,
    contentPadding = contentPadding + PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    itemsIndexed(
      items = items,
      key = { _, item -> item.id },
    ) { index, item ->
      LibraryItemCard(
        item = item,
        sharedTransitionKey = item.id + seriesName,
        sharedTransitionZIndex = -(index + 1f),
        offlineStatus = offlineStatus(item.id),
        onClick = { onLibraryItemClick(item) },
      )
    }
  }
}
