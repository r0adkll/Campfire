package app.campfire.author.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.author.ui.detail.composables.AuthorDetailHeader
import app.campfire.author.ui.detail.composables.AuthorHeader
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.offline.OfflineStatus
import campfire.features.author.ui.generated.resources.Res
import campfire.features.author.ui.generated.resources.author_books_header
import campfire.features.author.ui.generated.resources.error_author_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@CircuitInject(AuthorDetailScreen::class, UserScope::class)
@Composable
fun AuthorDetail(
  screen: AuthorDetailScreen,
  state: AuthorDetailUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(screen.authorName) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          IconButton(
            onClick = { state.eventSink(AuthorDetailUiEvent.Back) },
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
          }
        },
      )
    },
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    when (state.authorContentState) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_author_message),
        modifier = Modifier.padding(paddingValues),
      )

      is LoadState.Loaded -> LoadedState(
        author = state.authorContentState.data,
        offlineStatus = { state.offlineStates[it].asWidgetStatus() },
        contentPadding = paddingValues,
        onLibraryItemClick = { state.eventSink(AuthorDetailUiEvent.LibraryItemClick(it)) },
      )
    }
  }
}

@Composable
private fun LoadedState(
  author: Author,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  onLibraryItemClick: (LibraryItem) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  gridState: LazyGridState = rememberLazyGridState(),
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    state = gridState,
    contentPadding = contentPadding + PaddingValues(16.dp),
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(
      key = "header",
      span = { GridItemSpan(maxLineSpan) },
    ) {
      AuthorHeader(author)
    }

    item(
      key = "library_items_header",
      span = { GridItemSpan(maxLineSpan) },
    ) {
      AuthorDetailHeader(
        title = stringResource(Res.string.author_books_header),
      )
    }

    items(
      items = author.libraryItems,
      key = { it.id },
    ) { item ->
      LibraryItemCard(
        item = item,
        offlineStatus = offlineStatus(item.id),
        modifier = Modifier
          .clickable { onLibraryItemClick(item) },
      )
    }
  }
}
