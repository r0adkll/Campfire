// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings

import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import kotlinx.coroutines.CancellationException

/**
 * The Downloads page's rows for these downloads, most recently changed first, each with the item
 * [libraryItem] finds for it.
 */
internal suspend fun List<Download>.asDownloadEntries(
  libraryItem: suspend (LibraryItemId) -> LibraryItem,
): List<DownloadEntry> = mapNotNull { download ->
  // A download whose item can't be resolved (server unreachable with no cached copy,
  // or deleted on the server) shouldn't take the whole settings screen down.
  val item = try {
    libraryItem(download.content.itemId)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    bark(throwable = e) { "Unable to resolve library item for download ${download.content.itemId}" }
    return@mapNotNull null
  }
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
}.sortedByDescending { it.download.updatedAtMs }
