// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.ItemCollectionSharedTransitionKey
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.MaxBookDisplay
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.common.compose.widgets.adaptiveEnterAlwaysScrollBehavior
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.SeriesId
import app.campfire.core.offline.OfflineStatus
import app.campfire.series.ui.detail.composables.ConfirmSeriesProgressDialog
import app.campfire.series.ui.detail.composables.MissingSeriesBookCard
import app.campfire.series.ui.detail.composables.SeriesProgressAction
import app.campfire.series.ui.detail.composables.SeriesProgressMenu
import app.campfire.series.ui.detail.composables.seriesBookLabel
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.error_series_detail_message
import campfire.features.series.ui.generated.resources.missing_section_title
import com.slack.circuit.codegen.annotations.CircuitInject
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
  val scrollBehavior = adaptiveEnterAlwaysScrollBehavior()
  var confirmProgressAction by remember { mutableStateOf<SeriesProgressAction?>(null) }

  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(screen.seriesName) },
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets(),
        contentPadding = WindowInsets.statusBars
          .asPaddingValues(),
        navigationIcon = {
          NavigationBackButton(onClick = { state.eventSink(SeriesDetailUiEvent.Back) })
        },
        actions = {
          if (state.unfinishedCount > 0 || state.finishedCount > 0) {
            SeriesProgressMenu(
              unfinishedCount = state.unfinishedCount,
              finishedCount = state.finishedCount,
              enabled = !state.isUpdatingProgress,
              onActionClick = { confirmProgressAction = it },
            )
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
        seriesId = screen.seriesId,
        seriesName = screen.seriesName,
        items = state.seriesContentState.data,
        missingSection = state.missingSection,
        offlineStatus = { state.offlineStates[it].asWidgetStatus() },
        onLibraryItemClick = { state.eventSink(SeriesDetailUiEvent.LibraryItemClick(it)) },
        onMissingBookClick = { url -> state.eventSink(SeriesDetailUiEvent.MissingBookClick(url)) },
        contentPadding = paddingValues,
      )
    }
  }

  confirmProgressAction?.let { action ->
    ConfirmSeriesProgressDialog(
      action = action,
      bookCount = when (action) {
        SeriesProgressAction.MarkFinished -> state.unfinishedCount
        SeriesProgressAction.MarkNotFinished -> state.finishedCount
      },
      onConfirm = {
        confirmProgressAction = null
        state.eventSink(
          when (action) {
            SeriesProgressAction.MarkFinished -> SeriesDetailUiEvent.MarkSeriesFinished
            SeriesProgressAction.MarkNotFinished -> SeriesDetailUiEvent.MarkSeriesNotFinished
          },
        )
      },
      onDismiss = { confirmProgressAction = null },
    )
  }
}

@Composable
private fun LoadedState(
  seriesId: SeriesId,
  seriesName: String,
  items: List<LibraryItem>,
  missingSection: MissingSection?,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  onLibraryItemClick: (LibraryItem) -> Unit,
  onMissingBookClick: (String) -> Unit,
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
        // Same "Book 2 · 2014" line as the missing books, in place of the author.
        subtitle = seriesBookLabel(
          position = item.media.metadata.seriesSequence(seriesId)?.sequence,
          year = item.media.metadata.publishedYear,
        ),
        sharedTransitionKey = item.id + seriesName,
        sharedTransitionZIndex = (items.size - index) + 1f,
        offlineStatus = offlineStatus(item.id),
        onClick = { onLibraryItemClick(item) },
      )
    }

    if (missingSection != null) {
      item(span = { GridItemSpan(maxLineSpan) }, key = "missing-header") {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          Text(
            text = stringResource(Res.string.missing_section_title),
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }
      items(
        items = missingSection.books,
        key = { it.key },
      ) { missing ->
        MissingSeriesBookCard(
          entry = missing.entry,
          onClick = missing.entry.providerUrl?.let { url -> { onMissingBookClick(url) } },
        )
      }
    }
  }
}
