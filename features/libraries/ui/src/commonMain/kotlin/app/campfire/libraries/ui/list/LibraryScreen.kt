package app.campfire.libraries.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.LibraryScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.ui.appbar.CampfireAppBar
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
        { state.eventSink(LibraryUiEvent.OpenDrawer) },
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
      is LibraryContentState.Loaded -> LoadedState(
        items = state.contentState.items,
        modifier = Modifier.padding(paddingValues)
      )
    }
  }
}

@Composable
private fun LoadedState(
  items: List<LibraryItem>,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyGridState = rememberLazyGridState(),
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(3),
    state = state,
    modifier = modifier,
    contentPadding = contentPadding,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {

  }
}
