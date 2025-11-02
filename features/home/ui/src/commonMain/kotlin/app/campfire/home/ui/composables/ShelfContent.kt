package app.campfire.home.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.AuthorCard
import app.campfire.common.compose.widgets.ItemCollectionCard
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Series
import app.campfire.core.model.ShelfEntity
import app.campfire.core.offline.OfflineStatus
import app.campfire.home.ui.UiShelf
import campfire.features.home.ui.generated.resources.Res
import campfire.features.home.ui.generated.resources.shelf_content_error_message
import org.jetbrains.compose.resources.stringResource

private val LibraryCardWidth = 180.dp
private val SeriesCardWidth = 300.dp

@Composable
fun ShelfContent(
  shelf: UiShelf<ShelfEntity>,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  progressStatus: (LibraryItemId) -> MediaProgress?,
  onItemClick: (Any) -> Unit,
  modifier: Modifier = Modifier,
  state: LazyListState = rememberLazyListState(),
) {
  @Suppress("UNCHECKED_CAST")
  when (shelf.entities) {
    LoadState.Loading -> LoadingShelfContent(modifier)
    LoadState.Error -> ErrorShelfContent(shelf, modifier)
    is LoadState.Loaded<*> -> LoadedShelfContent(
      shelf = shelf,
      entities = shelf.entities.data as List<ShelfEntity>,
      offlineStatus = offlineStatus,
      progressStatus = progressStatus,
      onItemClick = onItemClick,
      modifier = modifier,
      state = state,
    )
  }
}

@Composable
private fun ShelfContentBox(
  modifier: Modifier = Modifier,
  contentAlignment: Alignment = Alignment.Center,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(LibraryCardWidth),
    contentAlignment = contentAlignment,
    content = content,
  )
}

@Composable
private fun LoadingShelfContent(
  modifier: Modifier = Modifier,
) {
  ShelfContentBox(
    modifier = modifier,
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ErrorShelfContent(
  shelf: UiShelf<*>,
  modifier: Modifier = Modifier,
) {
  ShelfContentBox(
    modifier = modifier,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 32.dp),
    ) {
      Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
      Text(
        text = stringResource(Res.string.shelf_content_error_message, shelf.label),
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun LoadedShelfContent(
  shelf: UiShelf<*>,
  entities: List<ShelfEntity>,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  progressStatus: (LibraryItemId) -> MediaProgress?,
  onItemClick: (Any) -> Unit,
  modifier: Modifier = Modifier,
  state: LazyListState = rememberLazyListState(),
) {
  LazyRow(
    state = state,
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 16.dp),
  ) {
    items(
      items = entities,
      key = { entity ->
        when (entity) {
          is LibraryItem -> entity.id
          is Author -> entity.id
          is Series -> entity.id
          else -> entity.hashCode().toString()
        }
      },
    ) { entity ->
      when (entity) {
        is LibraryItem -> LibraryItemCard(
          item = entity,
          // This is needed since this view can have duplicates of this LibraryItem
          // and can cause the shared transition stuff to glitch out. So lets key it
          // to the specific shelf
          sharedTransitionKey = entity.id + shelf.id,
          offlineStatus = offlineStatus(entity.id),
          progress = progressStatus(entity.id)
            ?: entity.userMediaProgress,
          modifier = Modifier
            .clickable { onItemClick(entity) }
            .width(LibraryCardWidth)
            .animateItem()
            .semantics {
              contentDescription = "HomeLibraryItem"
            },
        )

        is Author -> AuthorCard(
          author = entity,
          modifier = Modifier
            .clickable { onItemClick(entity) }
            .width(LibraryCardWidth)
            .animateItem(),
        )

        is Series -> ItemCollectionCard(
          name = entity.name,
          description = entity.description,
          items = entity.books ?: emptyList(),
          modifier = Modifier
            .clickable { onItemClick(entity) }
            .width(SeriesCardWidth)
            .animateItem(),
        )
      }
    }
  }
}
