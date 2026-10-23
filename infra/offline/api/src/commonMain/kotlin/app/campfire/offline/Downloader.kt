// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline

import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow

/**
 * Keeps one kind of [DownloadContent] on this device: fetches it, stores it, and reports on it.
 * Each kind's module contributes its downloader into a `Set<Downloader>` in the user scope, and
 * [Downloads] routes to the one that [handles] the content.
 *
 * Calls only ever get content this downloader [handles], except [deleteAll], which goes to every
 * downloader. Otherwise the members mean what they do on [Downloads].
 */
interface Downloader {

  fun handles(content: DownloadContent): Boolean

  fun observeAll(): Flow<List<Download>>

  fun observe(contents: Set<DownloadContent>): Flow<Map<DownloadContent, Download>>

  fun download(contents: List<DownloadContent>)

  fun stop(content: DownloadContent)

  fun delete(content: DownloadContent)

  suspend fun deleteAll(itemId: LibraryItemId)
}
