// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.campfire.core.model.LibraryItem
import app.campfire.widgets.R
import app.campfire.widgets.util.glanceStringResource
import coil3.size.Size
import kotlin.collections.chunked

@Composable
internal fun DiscoverWidgetContent(
  continueListeningShelf: List<LibraryItem>?,
  discoverShelf: List<LibraryItem>?,
  recentlyAddedShelf: List<LibraryItem>?,
  onClick: Action,
  onItemClick: (LibraryItem) -> Action,
  modifier: GlanceModifier = GlanceModifier,
) {
  PlayerWidgetScaffold(
    onClick = onClick,
    modifier = modifier,
    backgroundColor = GlanceTheme.colors.background,
    content = {
      DiscoverContent(
        continueListeningShelf = continueListeningShelf,
        discoverShelf = discoverShelf,
        recentlyAddedShelf = recentlyAddedShelf,
        onClick = onClick,
        onItemClick = onItemClick,
      )
    },
  )
}

@Composable
private fun DiscoverContent(
  continueListeningShelf: List<LibraryItem>?,
  discoverShelf: List<LibraryItem>?,
  recentlyAddedShelf: List<LibraryItem>?,
  onClick: Action,
  onItemClick: (LibraryItem) -> Action,
  modifier: GlanceModifier = GlanceModifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize(),
  ) {
    // App Title Bar
    Row(
      modifier = GlanceModifier
        .fillMaxWidth()
        .padding(
          horizontal = 16.dp,
          vertical = 16.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
        provider = ImageProvider(R.drawable.campfire_logo),
        contentDescription = "logo",
        modifier = GlanceModifier
          .size(32.dp),
      )
      Spacer(GlanceModifier.width(16.dp))
      Text(
        text = "Campfire",
        style = TextStyle(
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
        ),
      )
    }

    // Content
    val outerPadding = 16.dp
    val itemSpacing = 4.dp
    val totalSpacing = itemSpacing * (COLUMN_COUNT - 1)
    val cellSize = (LocalSize.current.width - totalSpacing) / COLUMN_COUNT

    LazyColumn(
      modifier = GlanceModifier
        .fillMaxSize(),
    ) {
      if (continueListeningShelf != null) {
        ShelfContent(
          title = { glanceStringResource(R.string.player_widget_continue_listening) },
          items = continueListeningShelf,
          onHeaderClick = onClick,
          onItemClick = onItemClick,
          itemSpacing = itemSpacing,
          itemSize = cellSize,
          outerPadding = outerPadding,
        )
      }

      if (discoverShelf != null) {
        ShelfContent(
          title = { glanceStringResource(R.string.player_widget_discover) },
          items = discoverShelf,
          onHeaderClick = onClick,
          onItemClick = onItemClick,
          itemSpacing = itemSpacing,
          itemSize = cellSize,
          outerPadding = outerPadding,
        )
      }

      if (recentlyAddedShelf != null) {
        ShelfContent(
          title = { glanceStringResource(R.string.player_widget_recently_added) },
          items = recentlyAddedShelf,
          onHeaderClick = onClick,
          onItemClick = onItemClick,
          itemSpacing = itemSpacing,
          itemSize = cellSize,
          outerPadding = outerPadding,
        )
      }
    }
  }
}

private fun LazyListScope.ShelfContent(
  title: @Composable () -> String,
  items: List<LibraryItem>,
  onHeaderClick: Action,
  onItemClick: (LibraryItem) -> Action,
  itemSpacing: Dp,
  itemSize: Dp,
  outerPadding: Dp,
  columnCount: Int = COLUMN_COUNT,
) {
  if (items.isEmpty()) return

  item {
    ShelfHeader(
      title = title(),
      modifier = GlanceModifier
        .clickable(onClick = onHeaderClick)
        .padding(
          horizontal = 16.dp,
          vertical = 12.dp,
        ),
    )
  }

  val chunks = items.chunked(columnCount)
  items(
    items = chunks,
  ) { chunk ->
    ShelfItemRow(
      chunk = chunk,
      itemSpacing = itemSpacing,
      itemSize = itemSize,
      onItemClick = onItemClick,
      modifier = GlanceModifier
        .padding(
          horizontal = outerPadding - itemSpacing,
          vertical = itemSpacing,
        ),
    )
  }
}

@Composable
private fun ShelfHeader(
  title: String,
  modifier: GlanceModifier = GlanceModifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
      ),
    )

    Spacer(GlanceModifier.defaultWeight())

    Image(
      provider = ImageProvider(R.drawable.ic_media_chevron_right),
      contentDescription = "open",
      modifier = GlanceModifier
        .size(24.dp),
    )
  }
}

@Composable
private fun ShelfItemRow(
  chunk: List<LibraryItem>,
  itemSpacing: Dp,
  itemSize: Dp,
  onItemClick: (LibraryItem) -> Action,
  modifier: GlanceModifier = GlanceModifier,
) {
  Row(
    modifier = modifier,
  ) {
    chunk.forEach { item ->
      Box(
        modifier = GlanceModifier
          .padding(horizontal = itemSpacing),
      ) {
        val pixelSize = with(Density(1.5f)) {
          itemSize.roundToPx()
        }
        GlanceImage(
          url = item.media.coverImageUrl,
          size = Size(pixelSize, pixelSize),
          modifier = GlanceModifier
            .clickable(onItemClick(item))
            .size(itemSize)
            .cornerRadius(8.dp),
          contentScale = ContentScale.Crop,
        )
      }
    }
  }
}

private const val COLUMN_COUNT = 3
