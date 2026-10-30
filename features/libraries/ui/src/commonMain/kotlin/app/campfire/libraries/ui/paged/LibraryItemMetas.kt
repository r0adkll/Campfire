// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.paged

import app.campfire.common.compose.widgets.LibraryItemMeta
import app.campfire.common.compose.widgets.defaultMeta
import app.campfire.core.extensions.seconds
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.LibraryItem
import app.campfire.core.settings.ContentSortMode
import kotlin.time.Duration

/**
 * A list row's bottom line: the item's [defaultMeta], then the facts that explain why it sits
 * where it does in the library — the value it's sorted by, and what the active filter narrows to
 * (like its place in a filtered series). Sorts and filters whose value is already on screen
 * (title, author) or shared by every result (genre, narrator) add nothing.
 */
internal fun LibraryItem.listMeta(
  sortMode: ContentSortMode,
  filter: ContentFilter?,
): List<LibraryItemMeta> = listOfNotNull(defaultMeta, sortMeta(sortMode), filterMeta(filter)).distinct()

private fun LibraryItem.sortMeta(sortMode: ContentSortMode): LibraryItemMeta? = when (sortMode) {
  ContentSortMode.PublishYear ->
    media.metadata.publishedYear
      ?.takeIf { it.isNotBlank() }
      ?.let { LibraryItemMeta.PublishedYear(it) }
  ContentSortMode.Size -> LibraryItemMeta.Size(media.sizeInBytes)
  ContentSortMode.AddedAt -> LibraryItemMeta.AddedAt(addedAtMillis)
  // Some filesystems don't record a creation time, which the server reports as 0
  ContentSortMode.FileCreated -> birthtimeMs.takeIf { it > 0 }?.let { LibraryItemMeta.FileCreated(it) }
  ContentSortMode.FileModified -> LibraryItemMeta.FileModified(mtimeMs)
  else -> null
}

private fun LibraryItem.filterMeta(filter: ContentFilter?): LibraryItemMeta? = when (filter) {
  is ContentFilter.Series -> media.metadata.seriesSequence(filter.value)
    ?.formattedSequence
    ?.takeIf { it.isNotEmpty() }
    ?.let { LibraryItemMeta.SeriesPosition(it) }
  is ContentFilter.Progress -> when (filter.type) {
    ContentFilter.Progress.Type.Finished ->
      userMediaProgress?.finishedAt
        ?.let { LibraryItemMeta.FinishedAt(it) }
    ContentFilter.Progress.Type.InProgress ->
      userMediaProgress
        ?.takeUnless { isEbookOnly || it.isFinished }
        ?.let { LibraryItemMeta.Remaining((media.duration - it.currentTime.seconds).coerceAtLeast(Duration.ZERO)) }
    else -> null
  }
  is ContentFilter.Tracks ->
    media.numTracks
      .takeIf { it > 0 }
      ?.let { LibraryItemMeta.Tracks(it) }
  else -> null
}
