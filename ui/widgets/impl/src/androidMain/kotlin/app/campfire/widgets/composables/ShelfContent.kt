package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.LibraryItem

@Composable
internal fun ColumnScope.ShelfContent(
  discoverShelf: List<LibraryItem>?,
  modifier: GlanceModifier = GlanceModifier,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .defaultWeight()
      .padding(horizontal = 4.dp),
  ) {
    discoverShelf?.let { discover ->
      item {
        Text(
          text = "Discover",
          style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
          ),
          modifier = GlanceModifier
            .padding(
              horizontal = 8.dp,
              vertical = 12.dp
            )
        )
      }
      itemsIndexed(
        items = discover,
        itemId = { _, item -> item.id.hashCode().toLong() },
      ) { index, item ->
        val isTop = index == 0
        val isBottom = index == discover.size - 1
        Box(
          modifier = GlanceModifier
            .padding(
              horizontal = 8.dp,
              vertical = 4.dp
            )
            .fluentIf(isBottom) {
              padding(bottom = 16.dp)
            }
        ) {
          LibraryItemCard(
            item = item,
          )
        }
      }
    }
  }
}

@Composable
private fun LibraryItemCard(
  item: LibraryItem,
  modifier: GlanceModifier = GlanceModifier,
  cornerRadius: Dp = 12.dp,
) {
  Row(
    modifier = modifier
      .background(GlanceTheme.colors.primaryContainer)
      .fillMaxWidth()
      .cornerRadius(cornerRadius),
  ) {
    val size = 64.dp

    GlanceImage(
      url = item.media.coverImageUrl,
      modifier = GlanceModifier
        .size(size)
        .cornerRadius(cornerRadius),
    )

    Column(
      modifier = GlanceModifier
        .defaultWeight()
        .height(size)
        .padding(
          horizontal = 16.dp,
//          vertical = 8.dp
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = item.media.metadata.title ?: "--",
        style = TextStyle(
          color = GlanceTheme.colors.onPrimaryContainer,
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
        ),
        maxLines = 1,
        modifier = GlanceModifier,
      )
      Spacer(GlanceModifier.height(4.dp))
      Text(
        text = item.media.metadata.authorName ?: "--",
        style = TextStyle(
          color = GlanceTheme.colors.onPrimaryContainer,
          fontSize = 14.sp,
          fontWeight = FontWeight.Normal,
        ),
        maxLines = 1,
        modifier = GlanceModifier,
      )
    }

  }
}
