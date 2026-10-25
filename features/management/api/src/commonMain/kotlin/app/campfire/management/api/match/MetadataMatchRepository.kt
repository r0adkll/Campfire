// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.api.match

import app.campfire.core.model.LibraryItemId
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.SeriesEntry
import kotlin.time.Duration

/** Searches the sources the server knows for a book's details, to match a book against. */
interface MetadataMatchRepository {

  /**
   * The sources the server can search, built-in and custom. Servers before 2.31 don't list
   * them, so for those it's the sources every server has.
   */
  suspend fun getBookProviders(): Result<List<MatchProvider>>

  /**
   * Search [provider] for books like [itemId], by [title], which can be an ASIN, and [author].
   * The server ranks Audible results by how well they match the item.
   */
  suspend fun searchBooks(
    itemId: LibraryItemId,
    provider: String,
    title: String,
    author: String?,
  ): Result<List<BookMatch>>

  /** The source searched last, to start from the next time */
  var lastProvider: String?

  /**
   * Have the server match the book [itemId] against the first result [provider] finds for it, by
   * the book's own title, author, ISBN and ASIN. It fills in what the book is missing, and a cover
   * when it has none; [replaceDetails] and [replaceCover] let it replace what the book has. A
   * changed book is fetched again here.
   *
   * @return a failure, an [ItemChangeNotAllowedException] when the account may not change items
   */
  suspend fun quickMatchBook(
    itemId: LibraryItemId,
    provider: String,
    replaceDetails: Boolean,
    replaceCover: Boolean,
  ): Result<QuickMatchResult>
}

/** What a quick match did to a book. */
sealed interface QuickMatchResult {

  /** The source found nothing for the book. */
  data object NoMatch : QuickMatchResult

  /** The source's first result had nothing for the book that it was allowed to change. */
  data object NothingChanged : QuickMatchResult

  /**
   * The server changed the book.
   *
   * @property undo What puts back the details the match changed, one field for each; null when the
   * changed book couldn't be fetched to see what they were.
   * @property coverChanged Whether the cover surely changed: the book had none, or nothing else
   * changed. A replaced cover keeps its file's name, so beside changed details it can't be told.
   */
  data class Updated(
    val undo: BookDetailsUpdate?,
    val coverChanged: Boolean,
  ) : QuickMatchResult
}

/** A source the server searches for a book's details, like Audible or Google Books */
data class MatchProvider(
  /** What the server calls it, like `audible.uk` or `custom-<id>` */
  val id: String,
  val name: String,
)

/** A book a source found, with whatever it has of the details; the rest are null or empty. */
data class BookMatch(
  val title: String? = null,
  val subtitle: String? = null,
  val authors: List<String> = emptyList(),
  val narrators: List<String> = emptyList(),
  val publisher: String? = null,
  val publishedYear: String? = null,
  /** HTML from some sources, plain text from others */
  val description: String? = null,
  /** On the source's own host */
  val coverUrl: String? = null,
  val isbn: String? = null,
  val asin: String? = null,
  val genres: List<String> = emptyList(),
  val tags: List<String> = emptyList(),
  val series: List<SeriesEntry> = emptyList(),
  val language: String? = null,
  val duration: Duration? = null,
  /** From 0 to 1, how well it matches the book searched for, which only Audible results have */
  val confidence: Float? = null,
  val explicit: Boolean? = null,
  val abridged: Boolean? = null,
)
