// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.api

import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow

/**
 * An ebook's table of contents, as the reader last found it in the file. The server doesn't know
 * an ebook's contents, so a book shows none until it has been opened on this device.
 */
interface EbookContentsRepository {

  fun observeContents(itemId: LibraryItemId): Flow<List<EbookContentsEntry>>

  /** Replace [itemId]'s contents with [entries], in reading order. */
  suspend fun saveContents(itemId: LibraryItemId, entries: List<EbookContentsEntry>)
}

data class EbookContentsEntry(
  val label: String,
  /** Where the entry points in the book, for [app.campfire.reader.api.screen.ReaderScreen.href]. */
  val href: String,
  /** How deeply it's nested, from 0. */
  val depth: Int,
  /** How far through the book it starts, 0..1, where the reader could tell. */
  val fraction: Float?,
)

/**
 * The entry a reader [progress] (0..1) through the book is in: the last one starting at or
 * before it, or null before the first.
 */
fun List<EbookContentsEntry>.entryAt(progress: Float): EbookContentsEntry? =
  lastOrNull { it.fraction != null && it.fraction <= progress }
