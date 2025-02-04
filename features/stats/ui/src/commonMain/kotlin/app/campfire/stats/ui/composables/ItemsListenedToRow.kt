package app.campfire.stats.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.core.model.ItemListenedTo
import app.campfire.stats.ui.StatsUiModel.ItemsListenedTo

internal val DefaultItemListenedToWidth = 88.dp

@Composable
internal fun ItemsListenedToRow(
  itemsListenedTo: ItemsListenedTo,
  onItemClick: (ItemListenedTo) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(
    horizontal = StatsDefaults.HorizontalPadding,
    vertical = StatsDefaults.VerticalPadding,
  ),
  state: LazyListState = rememberLazyListState(),
  itemWidth: Dp = DefaultItemListenedToWidth,
) {
  LazyRow(
    modifier = modifier,
    state = state,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = contentPadding,
  ) {
    items(
      items = itemsListenedTo.items,
      key = { it.id },
    ) {
      ListenedItemCard(
        item = it,
        onClick = { onItemClick(it) },
        modifier = Modifier
          .width(itemWidth)
          .animateItem(),
      )
    }
  }
}

@Composable
private fun ListenedItemCard(
  item: ItemListenedTo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    onClick = onClick,
    modifier = modifier,
  ) {
    ItemImage(
      imageUrl = item.coverImageUrl,
      contentDescription = item.mediaMetadata.title,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(CardDefaults.elevatedShape),
    )

    Text(
      text = item.timeListening.thresholdReadoutFormat(),
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(
          horizontal = 8.dp,
          vertical = 4.dp,
        ),
    )
  }
}
