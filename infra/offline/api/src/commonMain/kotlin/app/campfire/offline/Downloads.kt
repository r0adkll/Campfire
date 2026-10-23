// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline

import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Everything kept on this device to use offline, whatever its kind: audiobooks, podcast episodes
 * and ebooks. The one place features observe and change downloads; each kind is kept by its own
 * [Downloader], which also picks up its downloads cut short on its own platform's lifecycle.
 */
interface Downloads {

  /**
   * Every download, for lists of them. Changes of state show at once, but progress may only
   * show here as the download changes state; use [observe] to follow a download's progress.
   */
  fun observeAll(): Flow<List<Download>>

  /** The downloads of [contents] as they go, progress included; content not downloaded is absent. */
  fun observe(contents: Set<DownloadContent>): Flow<Map<DownloadContent, Download>>

  /** Starts downloading [contents], skipping any already downloaded or on their way. */
  fun download(contents: List<DownloadContent>)

  /** Stops [content]'s download part way, keeping what's been fetched. */
  fun stop(content: DownloadContent)

  /** Removes [content] from this device, stopping its download if it's still going. */
  fun delete(content: DownloadContent)

  /**
   * Removes everything of [itemId] from this device. Works without the item itself, for one that
   * no longer exists on the server or in the local cache.
   */
  suspend fun deleteAll(itemId: LibraryItemId)
}

/** [content]'s download as it goes, or null while it isn't downloaded. */
fun Downloads.observe(content: DownloadContent): Flow<Download?> =
  observe(setOf(content)).map { it[content] }

/** Every download, grouped by the item it belongs to. */
fun Downloads.observeByItem(): Flow<Map<LibraryItemId, ItemDownloads>> =
  observeAll().map { it.byItem() }

fun Downloads.download(content: DownloadContent) = download(listOf(content))
