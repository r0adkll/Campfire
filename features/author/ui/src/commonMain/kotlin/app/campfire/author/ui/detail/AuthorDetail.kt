package app.campfire.author.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.author.ui.detail.composables.AuthorHeader
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
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
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(screen.authorName) },
        navigationIcon = {
          IconButton(
            onClick = { state.eventSink(AuthorDetailUiEvent.Back) },
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
          }
        },
      )
    },
    modifier = modifier,
  ) { paddingValues ->
    when (state.authorContentState) {
      AuthorContentState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      AuthorContentState.Error -> ErrorListState(
        message = stringResource(Res.string.error_author_message),
        modifier = Modifier.padding(paddingValues),
      )
      is AuthorContentState.Loaded -> LoadedState(
        author = state.authorContentState.author,
        contentPadding = paddingValues,
        onLibraryItemClick = { state.eventSink(AuthorDetailUiEvent.LibraryItemClick(it)) },
      )
    }
  }
}

@Composable
private fun LoadedState(
  author: Author,
  onLibraryItemClick: (LibraryItem) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  gridState: LazyGridState = rememberLazyGridState(),
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(3),
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
      Box(
        modifier = Modifier.height(48.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        Text(
          text = stringResource(Res.string.author_books_header),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    items(
      items = author.libraryItems,
      key = { it.id },
    ) { item ->
      LibraryItemCard(
        item = item,
        modifier = Modifier
          .clickable { onLibraryItemClick(item) },
      )
    }
  }
}
