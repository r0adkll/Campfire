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
 * the field, which the server stores as no value.
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
) {
  val isEmpty: Boolean
    get() = this == BookDetailsUpdate()
}
