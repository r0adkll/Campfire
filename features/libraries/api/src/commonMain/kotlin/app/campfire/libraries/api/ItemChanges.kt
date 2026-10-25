// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.api

/** What the server found when it scanned an item's files again. */
enum class RescanResult {
  /** The files had changed, and the item now matches them. */
  Updated,

  /** The item already matched its files. */
  UpToDate,

  /** The server couldn't find the item or its library to scan. */
  Nothing,
}

/**
 * The server refused a change to an item because this account isn't allowed to make it, most
 * likely because an admin took a permission away since it signed in.
 */
class ItemChangeNotAllowedException(cause: Throwable? = null) :
  Exception("This account isn't allowed to change the item", cause)

/**
 * Changes to a book's details on the server. A null field stays as it is; an empty text clears
 * the field, which the server stores as no value. A list replaces the whole list.
 */
data class BookDetailsUpdate(
  val title: String? = null,
  val subtitle: String? = null,
  /** HTML, as the server keeps descriptions */
  val description: String? = null,
  val publisher: String? = null,
  val publishedYear: String? = null,
  val language: String? = null,
  val isbn: String? = null,
  val asin: String? = null,
  val explicit: Boolean? = null,
  val abridged: Boolean? = null,
  /** By name; the server links each to the library's author of that name, ignoring case, or adds one */
  val authors: List<String>? = null,
  val narrators: List<String>? = null,
  /** The server links each to the library's series of that name, ignoring case, or adds one */
  val series: List<SeriesEntry>? = null,
  val genres: List<String>? = null,
  val tags: List<String>? = null,
  /**
   * A cover for the server to download and use in place of the book's own, which takes the upload
   * permission as well
   */
  val coverUrl: String? = null,
) {
  val isEmpty: Boolean
    get() = this == BookDetailsUpdate()
}

/** A series a book is in, by name, with its place in it as the server keeps it: as text */
data class SeriesEntry(
  val name: String,
  val sequence: String? = null,
)
