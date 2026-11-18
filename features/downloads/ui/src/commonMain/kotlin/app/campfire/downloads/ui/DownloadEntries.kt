// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Downloads screen's rows as these downloads change. Each item is looked up with [libraryItem]
 * once, when its first download shows, and the lookups run side by side: a row shows as soon as its
 * own item is found, and a download's progress never waits on, or restarts, a lookup. An item that
 * can't be found shows as [DownloadEntry.Unavailable] while the screen is open.
 *
 * Nothing is emitted while there are downloads but none of their items has been looked up yet, so
 * the list never looks empty before its first row is ready.
 */
internal fun Flow<List<Download>>.asDownloadEntries(
  libraryItem: suspend (LibraryItemId) -> LibraryItem,
): Flow<List<DownloadEntry>> = channelFlow {
  // Null for an item that couldn't be found
  val items = MutableStateFlow<Map<LibraryItemId, LibraryItem?>>(emptyMap())
  val lookedUp = mutableSetOf<LibraryItemId>()
  val downloads = onEach { downloads ->
    downloads.map { it.content.itemId }.filter(lookedUp::add).forEach { itemId ->
      launch {
        val item = findItem(itemId, libraryItem)
        items.update { it + (itemId to item) }
      }
    }
  }
  combine(downloads, items) { downloads, items ->
    val entries = downloads.asDownloadEntries(items)
    val lookingUp = downloads.any { it.content.itemId !in items }
    if (entries.isEmpty() && lookingUp) null else entries
  }
    .filterNotNull()
    .collect { send(it) }
}

/**
 * The Downloads screen's rows for these downloads whose [items] have been looked up, most recently
 * changed first. An item looked up and not found, null in [items], gets one
 * [DownloadEntry.Unavailable] row for all of its downloads.
 */
internal fun List<Download>.asDownloadEntries(
  items: Map<LibraryItemId, LibraryItem?>,
): List<DownloadEntry> {
  val lookedUp = filter { it.content.itemId in items }
  val found = lookedUp.mapNotNull { download ->
    val item = items[download.content.itemId] ?: return@mapNotNull null
    when (val content = download.content) {
      is DownloadContent.Audiobook -> DownloadEntry.Book(item, download)
      is DownloadContent.Ebook -> DownloadEntry.Ebook(item, download)
      is DownloadContent.Episode -> {
        val media = item.media as? Media.Podcast ?: return@mapNotNull null
        val episode = media.episodes.find { it.id == content.episodeId }
          ?: return@mapNotNull null
        DownloadEntry.Episode(item, episode, download)
      }
    }
  }
  val unavailable = lookedUp
    .filter { items[it.content.itemId] == null }
    .groupBy { it.content.itemId }
    .map { (_, downloads) ->
      DownloadEntry.Unavailable(
        download = downloads.maxBy { it.updatedAtMs },
        sizeInBytes = downloads.sumOf { it.sizeOnDevice },
      )
    }
  return (found + unavailable).sortedByDescending { it.download.updatedAtMs }
}

/** What the download takes up, as far as it's known: its whole size, or else what's fetched so far. */
private val Download.sizeOnDevice: Long
  get() = maxOf(totalBytes, bytes, 0L)

// A download whose item can't be resolved (server unreachable with no cached copy,
// or deleted on the server) shouldn't take the whole Downloads screen down.
private suspend fun findItem(
  itemId: LibraryItemId,
  libraryItem: suspend (LibraryItemId) -> LibraryItem,
): LibraryItem? = try {
  libraryItem(itemId)
} catch (e: CancellationException) {
  throw e
} catch (e: Exception) {
  bark(tag = "DownloadEntries", throwable = e) { "Unable to resolve library item for download $itemId" }
  null
}
