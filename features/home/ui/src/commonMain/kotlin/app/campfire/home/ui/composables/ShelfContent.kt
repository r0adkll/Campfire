package app.campfire.home.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.SeriesCard
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.home.api.model.Shelf

private val LibraryCardWidth = 180.dp
private val SeriesCardWidth = 300.dp

@Composable
fun ShelfContent(
  shelf: Shelf<*>,
  modifier: Modifier = Modifier,
  state: LazyListState = rememberLazyListState(),
) {
  LazyRow(
    state = state,
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 16.dp),
  ) {
    items(shelf.entities) { entity ->
      when (entity) {
        is LibraryItem -> LibraryItemCard(
          item = entity,
          modifier = Modifier.width(LibraryCardWidth),
        )
        is Author -> Unit
        is Series -> SeriesCard(
          series = entity,
          modifier = Modifier.width(SeriesCardWidth)
        )
      }
    }
  }
}
