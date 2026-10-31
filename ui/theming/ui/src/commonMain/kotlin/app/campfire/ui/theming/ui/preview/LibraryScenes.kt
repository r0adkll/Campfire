// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.DefaultAdaptiveColumnSize
import app.campfire.common.compose.layout.DenseAdaptiveColumnSize
import app.campfire.common.compose.widgets.FilterBar
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LibraryItemListItem
import app.campfire.core.settings.ItemDisplayState
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_preview_book_count
import campfire.ui.theming.ui.generated.resources.theme_preview_continue_listening
import campfire.ui.theming.ui.generated.resources.theme_preview_recently_added
import campfire.ui.theming.ui.generated.resources.theme_preview_show_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Home, condensed: the search bar, then as many shelves of the app's own cards as fit, over the
 * mini player and the navigation bar.
 */
@Composable
internal fun HomeScene(
  content: PreviewContent,
  icon: AppTheme.Icon,
  targets: ThemeTargets,
  onCoverThemingClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val library = content.library
  Column(modifier.fillMaxSize()) {
    PreviewAppBar(icon, targets)

    ClippedArea {
      PreviewShelfRow(
        label = library.continueListening.label ?: stringResource(Res.string.theme_preview_continue_listening),
        items = library.continueListening.items,
        targets = targets,
      )
      PreviewShelfRow(
        label = library.recentlyAdded.label ?: stringResource(Res.string.theme_preview_recently_added),
        items = library.recentlyAdded.items,
        targets = targets,
      )
    }

    val nowPlaying = library.nowPlaying
    if (content.playerUsesCoverColors) {
      CoverColorsNote(onSettingsClick = onCoverThemingClick, modifier = Modifier.padding(8.dp))
    } else if (nowPlaying != null) {
      PreviewMiniPlayer(nowPlaying, targets, Modifier.padding(8.dp))
    }
    PreviewNavigationBar(PreviewTab.Home, library.isPodcastLibrary, targets)
  }
}

/**
 * Library, condensed: the count, sort and layout bar over as many of the books as fit, laid out
 * as the user has chosen, over the navigation bar.
 */
@Composable
internal fun LibraryScene(
  content: PreviewContent,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val library = content.library
  val isGrid = library.displayState != ItemDisplayState.List
  Column(modifier.fillMaxSize()) {
    FilterBar(
      count = {
        val count = library.itemCount ?: library.allItems.size
        val label = if (library.isPodcastLibrary) {
          Res.plurals.theme_preview_show_count
        } else {
          Res.plurals.theme_preview_book_count
        }
        Text(pluralStringResource(label, count, count))
      },
      itemDisplayState = library.displayState,
      sortMode = library.sortMode,
      sortDirection = library.sortDirection,
      onSortClick = {},
      onDisplayStateClick = {},
      onFilterClick = {},
      modifier = Modifier
        .padding(top = 8.dp)
        // The grid and its bar sit inside the app's 16dp side padding; the list runs edge to edge
        .padding(horizontal = if (isGrid) 16.dp else 0.dp)
        .themeTarget(ContrastCheck.ScreenText, targets),
    )

    ClippedArea {
      if (isGrid) {
        PreviewGrid(
          items = library.allItems,
          dense = library.displayState == ItemDisplayState.GridDense,
          targets = targets,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          library.allItems.forEach { entry ->
            LibraryItemListItem(
              libraryItem = entry.item,
              onClick = {},
              sharedTransitionKey = PreviewTransitionKey + entry.key,
            )
          }
        }
      }
    }

    PreviewNavigationBar(PreviewTab.Library, library.isPodcastLibrary, targets)
  }
}

/**
 * The flexible middle of a condensed screen: it takes the room left between the fixed bars, and
 * what doesn't fit is cut off at the bottom instead of scrolling.
 */
@Composable
private fun ColumnScope.ClippedArea(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Box(
    modifier = modifier
      .weight(1f)
      .fillMaxWidth()
      .clipToBounds(),
  ) {
    Column(
      modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = true),
      content = content,
    )
  }
}

/** A Home shelf: its name over a row of cards, as `ShelfListItem` draws it. */
@Composable
private fun PreviewShelfRow(
  label: String,
  items: List<PreviewShelfItem>,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  if (items.isEmpty()) return
  Column(modifier.padding(bottom = 8.dp)) {
    Box(
      modifier = Modifier
        .height(48.dp)
        .padding(horizontal = 16.dp)
        .themeTarget(ContrastCheck.ScreenText, targets),
      contentAlignment = Alignment.Center,
    ) {
      Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
    // The row runs off the right edge, as a shelf does in the app
    Row(
      modifier = Modifier
        .wrapContentWidth(Alignment.Start, unbounded = true)
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items.take(MaxShelfCards).forEach { entry ->
        PreviewCard(entry, targets, Modifier.width(ShelfCardWidth))
      }
    }
  }
}

/** The Library grid: as many columns as fit at the app's adaptive column width. */
@Composable
private fun PreviewGrid(
  items: List<PreviewShelfItem>,
  dense: Boolean,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier.fillMaxWidth()) {
    val minColumn = if (dense) DenseAdaptiveColumnSize else DefaultAdaptiveColumnSize
    val columns = ((maxWidth + GridSpacing) / (minColumn + GridSpacing)).toInt().coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(GridSpacing)) {
      items.chunked(columns).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(GridSpacing)) {
          row.forEach { entry ->
            PreviewCard(
              entry = entry,
              targets = targets,
              showInformation = !dense,
              dense = dense,
              modifier = Modifier.weight(1f),
            )
          }
          repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
      }
    }
  }
}

/**
 * Campfire's own book card, with the parts the readability checks cover marked over it: the
 * progress bar along the bottom of the cover, and the title and author under it.
 */
@Composable
private fun PreviewCard(
  entry: PreviewShelfItem,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
  showInformation: Boolean = true,
  dense: Boolean = false,
) {
  BoxWithConstraints(modifier) {
    LibraryItemCard(
      item = entry.item,
      episode = entry.episode,
      sharedTransitionKey = PreviewTransitionKey + entry.key,
      showInformation = showInformation,
      marqueeEnabled = false,
      shape = if (dense) MaterialTheme.shapes.medium else MaterialTheme.shapes.largeIncreased,
    )

    val coverSize = maxWidth
    val progress = entry.item.userMediaProgress
    if (progress != null && !progress.isFinished) {
      val barHeight = if (showInformation) LargeBarHeight else SmallBarHeight
      Box(
        Modifier
          .padding(top = coverSize - barHeight)
          .height(barHeight)
          .fillMaxWidth()
          .themeTarget(ContrastCheck.ProgressBars, targets),
      )
    }
    if (showInformation) {
      Box(
        Modifier
          .padding(top = coverSize)
          .matchParentSize()
          .themeTarget(ContrastCheck.CardText, targets),
      )
    }
  }
}

/** Keeps the preview's cards from sharing transitions with the same books elsewhere. */
private const val PreviewTransitionKey = "theme-preview:"

/** Enough to run off the right edge of the widest preview. */
private const val MaxShelfCards = 6

private val ShelfCardWidth = 136.dp
private val GridSpacing = 8.dp
private val LargeBarHeight = 12.dp
private val SmallBarHeight = 8.dp
