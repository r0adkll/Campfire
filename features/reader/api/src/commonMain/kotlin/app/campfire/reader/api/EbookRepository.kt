// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.api

import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow

interface EbookRepository {

  /**
   * [itemId]'s primary ebook on this device, fetching it first unless it was downloaded or opened
   * before, along with the newer of the reading positions saved here and on the server.
   */
  suspend fun open(itemId: LibraryItemId): Result<Ebook>

  /**
   * Save the reading position for [itemId], here at once and to the server when the reader
   * settles on a page (or, offline, once the server can be reached again).
   *
   * @param location the position in the format the official Audiobookshelf readers use: an
   * epub.js CFI for EPUB, a 1-based page number for comics
   * @param progress how far through the book, 0..1
   */
  suspend fun saveProgress(itemId: LibraryItemId, location: String, progress: Float): Result<Unit>

  /** Whether [itemId]'s ebook is downloaded to read offline, or on its way. */
  fun observeDownload(itemId: LibraryItemId): Flow<EbookDownload>

  /** The items whose ebook is downloaded to read offline, or on its way. */
  fun observeDownloads(): Flow<Set<LibraryItemId>>

  /** Keep [itemId]'s ebook on this device to read offline. */
  fun download(itemId: LibraryItemId)

  /** Remove [itemId]'s downloaded ebook. */
  fun deleteDownload(itemId: LibraryItemId)

  /** Remove every copy of [itemId]'s ebook from this device, for an item gone from the server. */
  suspend fun deleteFiles(itemId: LibraryItemId)
}

data class Ebook(
  val itemId: LibraryItemId,
  val title: String,
  val author: String?,
  /** The server's `ebookFormat`, e.g. `epub`, `pdf`, `cbz`. */
  val format: String,
  /** Absolute path of the file on this device. */
  val filePath: String,
  /** The saved position, in the format described on [EbookRepository.saveProgress]. */
  val location: String?,
  val progress: Float?,
)

data class EbookDownload(
  val state: State = State.None,
  val bytes: Long = 0L,
  /** The file's size, or -1 until it's known. */
  val totalBytes: Long = -1L,
) {
  enum class State { None, Downloading, Downloaded, Failed }

  companion object {
    val None = EbookDownload()
  }
}
