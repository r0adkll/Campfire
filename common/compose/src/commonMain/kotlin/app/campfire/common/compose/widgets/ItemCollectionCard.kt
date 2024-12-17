package app.campfire.common.compose.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastSumBy
import app.campfire.core.model.LibraryItem

private val BookImageSize = 180.dp
private val BookCornerSize = 12.dp
private const val MaxBookDisplay = 8

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemCollectionCard(
  name: String,
  description: String?,
  items: List<LibraryItem>,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier,
  ) {
    MultiBookLayout(
      items = items,
      modifier = Modifier
        .background(MaterialTheme.colorScheme.primaryContainer)
        .fillMaxWidth()
        .clip(RoundedCornerShape(BookCornerSize)),
    )
    Column(
      Modifier.padding(
        horizontal = 16.dp,
        vertical = 16.dp,
      ),
    ) {
      Text(
        text = name,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        modifier = Modifier.basicMarquee(),
      )
      description?.let { desc ->
        Text(
          text = desc,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 2,
          modifier = Modifier.basicMarquee(),
        )
      }
    }
  }
}

@Composable
private fun MultiBookLayout(
  items: List<LibraryItem>,
  modifier: Modifier = Modifier,
) {
  Layout(
    content = {
      items
        .sortedBy { it.media.metadata.seriesSequence?.sequence }
        .take(MaxBookDisplay)
        .forEach { item ->
          ItemImage(
            imageUrl = item.media.coverImageUrl,
            contentDescription = item.media.metadata.title,
            modifier = Modifier.size(BookImageSize),
          )
        }
    },
    modifier = modifier,
  ) { measurables, constraints ->
    val ezConstraints = constraints.copy(minWidth = 0, minHeight = 0)
    val placeables = measurables.map { it.measure(ezConstraints) }

    // Grab the total width
    val totalItemWidth = placeables.fastSumBy { it.measuredWidth }
    val maxItemWidth = placeables.fastMaxBy { it.measuredWidth }?.measuredWidth ?: BookImageSize.roundToPx()
    val maxItemHeight = placeables.fastMaxBy { it.measuredHeight }?.measuredHeight ?: constraints.minHeight

    // Compute the item offset amount
    val smallOffset = (constraints.maxWidth - totalItemWidth) / 2

    layout(constraints.maxWidth, maxItemHeight) {
      if (totalItemWidth < constraints.maxWidth) {
        // Too few items to offset, just lay them out like a center-aligned row
        var widthOffset = 0
        placeables.fastForEach { placeable ->
          placeable.place(smallOffset + widthOffset, 0)
          widthOffset += placeable.width
        }
      } else {
        val itemOffset = (constraints.maxWidth - maxItemWidth) / (measurables.size - 1)
        // Otherwise, layer them like an accordion
        placeables.fastForEachIndexed { index, placeable ->
          placeable.place(index * itemOffset, 0, zIndex = -index.toFloat())
        }
      }
    }
  }
}
