// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media

/**
 * How a library item is identified against an external provider's catalog.
 */
sealed interface BookMatch {
  /**
   * All hard identifiers the item carries (at least one is non-null).
   * Providers should try every identifier they support — individual identifier
   * coverage varies per catalog (e.g. audiobook ASINs are frequently missing).
   */
  data class Identifiers(val isbn: String?, val asin: String?) : BookMatch

  data class TitleAuthor(val title: String, val author: String?) : BookMatch

  /** Stable string form, used to detect when an item's identifiers change. */
  val cacheKey: String
    get() = when (this) {
      is Identifiers -> "isbn:${isbn.orEmpty()};asin:${asin.orEmpty()}"
      is TitleAuthor -> "title:$title;author:${author.orEmpty()}"
    }
}

/**
 * Derives the strongest available [BookMatch] from the item's Audiobookshelf
 * metadata, or null when the item carries nothing an external catalog can key on.
 */
fun LibraryItem.bestMatch(): BookMatch? = media.metadata.bestMatch()

fun Media.Metadata.bestMatch(): BookMatch? {
  val isbn = ISBN?.takeUnless { it.isBlank() }
  val asin = ASIN?.takeUnless { it.isBlank() }
  if (isbn != null || asin != null) {
    return BookMatch.Identifiers(isbn = isbn, asin = asin)
  }
  val title = title?.takeUnless { it.isBlank() } ?: return null
  return BookMatch.TitleAuthor(title = title, author = authorName?.takeUnless { it.isBlank() })
}
