package app.campfire.collections.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
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
import app.campfire.common.screens.CollectionsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.Collection
import app.campfire.ui.appbar.CampfireAppBar
import campfire.features.collections.ui.generated.resources.Res
import campfire.features.collections.ui.generated.resources.empty_collection_items_message
import campfire.features.collections.ui.generated.resources.error_collection_items_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(CollectionsScreen::class, UserScope::class)
@Composable
fun Collections(
  state: CollectionsUiState,
  campfireAppBar: CampfireAppBar,
  modifier: Modifier = Modifier,
) {
  val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)
  val appBarBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  val gridState = rememberLazyGridState()

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
    contentWindowInsets = CampfireWindowInsets.exclude(WindowInsets.navigationBars),
  ) { paddingValues ->
    when (state.collectionContentState) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_collection_items_message),
        modifier = Modifier.padding(paddingValues),
      )

      is LoadState.Loaded -> LoadedState(
        items = state.collectionContentState.data,
        onCollectionClick = { state.eventSink(CollectionsUiEvent.CollectionClick(it)) },
        contentPadding = paddingValues,
        state = gridState,
      )
    }
  }
}

@Composable
private fun LoadedState(
  items: List<Collection>,
  onCollectionClick: (Collection) -> Unit,
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
      contentPadding = contentPadding +
        PaddingValues(ContentPadding) +
        PaddingValues(bottom = FabBottomOffset),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      items(
        items = items,
        key = { it.id },
      ) { collection ->
        ItemCollectionCard(
          name = collection.name,
          description = collection.description,
          items = collection.books,
          modifier = Modifier
            .fillMaxWidth()
            .animateItem()
            .clickable { onCollectionClick(collection) },
        )
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
