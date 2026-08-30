// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.ItemCollectionSharedTransitionKey
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.MaxBookDisplay
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.offline.OfflineStatus
import app.campfire.series.ui.detail.composables.ProviderEntryCard
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.action_back
import campfire.features.series.ui.generated.resources.error_series_detail_message
import campfire.features.series.ui.generated.resources.series_source_attribution
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@CircuitInject(SeriesDetailScreen::class, UserScope::class)
@Composable
fun SeriesDetail(
  screen: SeriesDetailScreen,
  state: SeriesDetailUiState,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(screen.seriesName) },
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets(),
        contentPadding = WindowInsets.statusBars
          .asPaddingValues(),
        navigationIcon = {
          val backLabel = stringResource(Res.string.action_back)
          IconButtonTooltip(text = backLabel) {
            IconButton(
              onClick = { state.eventSink(SeriesDetailUiEvent.Back) },
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backLabel)
            }
          }
        },
      )
    },
    modifier = modifier
      .sharedBounds(
        sharedContentState = rememberSharedContentState(
          ItemCollectionSharedTransitionKey(
            id = screen.seriesId,
            type = ItemCollectionSharedTransitionKey.ElementType.Bounds,
          ),
        ),
        animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
        zIndexInOverlay = -(MaxBookDisplay + 1).toFloat(),
      )
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    when (state.seriesContentState) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_series_detail_message),
        modifier = Modifier.padding(paddingValues),
      )

      is LoadState.Loaded -> LoadedState(
        seriesName = screen.seriesName,
        info = state.seriesContentState.data,
        offlineStatus = { state.offlineStates[it].asWidgetStatus() },
        onLibraryItemClick = { state.eventSink(SeriesDetailUiEvent.LibraryItemClick(it)) },
        onProviderEntryClick = { state.eventSink(SeriesDetailUiEvent.ProviderEntryClick(it)) },
        contentPadding = paddingValues,
      )
    }
  }
}

@Composable
private fun LoadedState(
  seriesName: String,
  info: SeriesInfoState,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  onLibraryItemClick: (LibraryItem) -> Unit,
  onProviderEntryClick: (ProviderSeriesEntry) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  gridState: LazyGridState = rememberLazyGridState(),
) {
  val entries = info.entries
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    state = gridState,
    modifier = modifier,
    contentPadding = contentPadding + PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    itemsIndexed(
      items = entries,
      key = { _, entry -> entry.key },
    ) { index, entry ->
      when (entry) {
        is SeriesEntry.Owned -> LibraryItemCard(
          item = entry.item,
          sharedTransitionKey = entry.item.id + seriesName,
          sharedTransitionZIndex = (entries.size - index) + 1f,
          offlineStatus = offlineStatus(entry.item.id),
          onClick = { onLibraryItemClick(entry.item) },
        )

        is SeriesEntry.Missing -> ProviderEntryCard(
          entry = entry.entry,
          isUpcoming = false,
          onClick = { onProviderEntryClick(entry.entry) },
        )

        is SeriesEntry.Upcoming -> ProviderEntryCard(
          entry = entry.entry,
          isUpcoming = true,
          onClick = { onProviderEntryClick(entry.entry) },
        )
      }
    }

    info.providerName?.let { providerName ->
      item(key = "series_source_attribution", span = { GridItemSpan(maxLineSpan) }) {
        Text(
          text = stringResource(Res.string.series_source_attribution, providerName),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        )
      }
    }
  }
}
