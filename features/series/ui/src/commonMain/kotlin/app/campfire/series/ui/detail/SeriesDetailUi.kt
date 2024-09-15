package app.campfire.series.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
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
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(screen.seriesName) },
        navigationIcon = {
          IconButton(
            onClick = { state.eventSink(SeriesDetailUiEvent.Back) },
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
          }
        },
      )
    },
    modifier = modifier,
  ) { paddingValues ->
    when (state.seriesContentState) {
      SeriesContentState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      SeriesContentState.Error -> ErrorListState(
        message = stringResource(Res.string.error_series_detail_message),
        modifier = Modifier.padding(paddingValues),
      )
      is SeriesContentState.Loaded -> LoadedState(
        items = state.seriesContentState.items,
        onLibraryItemClick = { state.eventSink(SeriesDetailUiEvent.LibraryItemClick(it)) },
        contentPadding = paddingValues,
      )
    }
  }
}

@Composable
private fun LoadedState(
  items: List<LibraryItem>,
  onLibraryItemClick: (LibraryItem) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  gridState: LazyGridState = rememberLazyGridState(),
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(3),
    state = gridState,
    modifier = modifier,
    contentPadding = contentPadding + PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(
      items = items,
      key = { it.id },
    ) { item ->
      LibraryItemCard(
        item = item,
        modifier = Modifier.clickable {
          onLibraryItemClick(item)
        }
      )
    }
  }
}
