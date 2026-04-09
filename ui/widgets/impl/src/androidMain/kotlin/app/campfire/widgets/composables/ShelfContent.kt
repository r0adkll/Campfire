package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.campfire.core.model.LibraryItem

private const val COLUMN_COUNT = 3

@Composable
internal fun ColumnScope.ShelfContent(
  content: List<LibraryItem>?,
  modifier: GlanceModifier = GlanceModifier,
) {
  if (content == null) return

  val outerPadding = 12.dp
  val itemSpacing = 4.dp
  val totalSpacing = outerPadding * 2 + itemSpacing * (COLUMN_COUNT - 1)
  val cellSize = (LocalSize.current.width - totalSpacing) / COLUMN_COUNT

  Column(
    modifier = modifier
      .fillMaxWidth()
      .defaultWeight()
      .padding(horizontal = outerPadding),
  ) {
    Text(
      text = "Discover",
      style = TextStyle(
        color = GlanceTheme.colors.onSurface,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
      ),
      modifier = GlanceModifier
        .padding(vertical = 12.dp),
    )

    LazyVerticalGrid(
      gridCells = GridCells.Fixed(COLUMN_COUNT),
      modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
    ) {
      items(
        items = content,
        itemId = { item -> item.id.hashCode().toLong() },
      ) { item ->
        GlanceImage(
          url = item.media.coverImageUrl,
          modifier = GlanceModifier
            .size(cellSize)
            .cornerRadius(8.dp),
          contentScale = ContentScale.Crop,
        )
      }
    }
  }
}
