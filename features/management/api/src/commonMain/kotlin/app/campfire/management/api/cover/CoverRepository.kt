// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.api.cover

import app.campfire.core.model.LibraryItemId

/** Finds covers for a book: from the sources the server knows, and among the book's own files. */
interface CoverRepository {

  /**
   * Search [provider] for a book's covers by [title] and [author].
   *
   * @return the covers' addresses, on each source's own host
   */
  suspend fun searchCovers(provider: String, title: String, author: String?): Result<List<String>>

  /**
   * The images among the book [itemId]'s own files, as the server has them now. The app doesn't
   * keep a book's files, so these are always fetched.
   */
  suspend fun getBookImages(itemId: LibraryItemId): Result<List<BookImage>>
}

/** An image among a book's own files, which can be its cover. */
data class BookImage(
  val name: String,
  /** Where it is on the server, which is how a cover is set from it */
  val path: String,
  /** Where to load it from, through the server */
  val url: String,
  val sizeBytes: Long,
  /** Whether it's the book's cover now */
  val isCover: Boolean,
)
