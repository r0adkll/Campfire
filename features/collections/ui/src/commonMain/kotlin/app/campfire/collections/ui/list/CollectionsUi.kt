package app.campfire.collections.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.layout.DefaultAdaptiveColumnSize
import app.campfire.common.compose.layout.LargeAdaptiveColumnSize
import app.campfire.common.compose.layout.LazyCampfireGrid
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.FilterBar
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.ItemCollectionCard
import app.campfire.common.compose.widgets.ItemCollectionGridCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.CollectionsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Collection
import app.campfire.core.settings.GroupDisplayState
import app.campfire.core.settings.ItemDisplayState
import campfire.features.collections.ui.generated.resources.Res
import campfire.features.collections.ui.generated.resources.action_back
import campfire.features.collections.ui.generated.resources.collections_title
import campfire.features.collections.ui.generated.resources.empty_collection_items_message
import campfire.features.collections.ui.generated.resources.error_collection_items_message
import campfire.features.collections.ui.generated.resources.filter_bar_collection_count
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@CircuitInject(CollectionsScreen::class, UserScope::class)
@Composable
fun Collections(
  state: CollectionsUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val gridState = rememberLazyGridState()

  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(stringResource(Res.string.collections_title)) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          val backLabel = stringResource(Res.string.action_back)
          IconButtonTooltip(text = backLabel) {
            IconButton(
              onClick = { state.eventSink(CollectionsUiEvent.Back) },
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backLabel)
            }
          }
        },
      )
    },
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    when (state.collectionContentState) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_collection_items_message),
        modifier = Modifier.padding(paddingValues),
      )

      is LoadState.Loaded -> LoadedState(
        items = state.collectionContentState.data,
        displayState = state.displayState,
        onCollectionClick = { state.eventSink(CollectionsUiEvent.CollectionClick(it)) },
        onToggleDisplayState = { state.eventSink(CollectionsUiEvent.ToggleDisplayState) },
        contentPadding = paddingValues,
        state = gridState,
      )
    }
  }
}

@Composable
private fun LoadedState(
  items: List<Collection>,
  displayState: GroupDisplayState,
  onCollectionClick: (Collection) -> Unit,
  onToggleDisplayState: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyGridState = rememberLazyGridState(),
) {
  val adaptiveColumnSize: Dp = when (displayState) {
    GroupDisplayState.List -> LargeAdaptiveColumnSize
    GroupDisplayState.Grid -> DefaultAdaptiveColumnSize
  }

  Box(
    modifier = modifier.fillMaxSize(),
  ) {
    LazyCampfireGrid(
      columns = GridCells.Adaptive(adaptiveColumnSize),
      state = state,
      contentPadding = contentPadding +
        PaddingValues(horizontal = ContentPadding) +
        PaddingValues(bottom = FabBottomOffset),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      item(
        span = { GridItemSpan(maxLineSpan) },
        key = "filter-bar",
      ) {
        FilterBar(
          count = {
            Text(pluralStringResource(Res.plurals.filter_bar_collection_count, items.size, items.size))
          },
          itemDisplayState = when (displayState) {
            GroupDisplayState.List -> ItemDisplayState.List
            GroupDisplayState.Grid -> ItemDisplayState.Grid
          },
          onDisplayStateClick = onToggleDisplayState,
        )
      }

      items(
        count = items.size,
        key = { items[it].id },
      ) { index ->
        val collection = items[index]
        when (displayState) {
          GroupDisplayState.List -> ItemCollectionCard(
            sharedTransitionKey = collection.id,
            name = collection.name,
            description = collection.description,
            items = collection.books,
            onClick = { onCollectionClick(collection) },
            modifier = Modifier
              .fillMaxWidth()
              .animateItem(),
          )
          GroupDisplayState.Grid -> ItemCollectionGridCard(
            sharedTransitionKey = collection.id,
            name = collection.name,
            items = collection.books,
            onClick = { onCollectionClick(collection) },
            modifier = Modifier
              .fillMaxWidth()
              .animateItem(),
          )
        }
      }
    }

    if (items.isEmpty()) {
      EmptyState(
        message = stringResource(Res.string.empty_collection_items_message),
      )
    }
  }
}

private val ContentPadding = 16.dp
private val FabBottomOffset = 88.dp
