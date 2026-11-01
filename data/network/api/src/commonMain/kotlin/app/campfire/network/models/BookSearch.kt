// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** The body of `GET /api/search/providers`, which servers from 2.31 answer */
@Serializable
data class SearchProvidersResponse(
  val providers: SearchProviders = SearchProviders(),
)

@Serializable
data class SearchProviders(
  /** The sources a book's details can be matched from, built-in and custom */
  val books: List<SearchProvider> = emptyList(),
)

@Serializable
data class SearchProvider(
  /** What `provider` takes in a search, like `audible.uk` or `custom-<id>` */
  val value: String,
  val text: String,
)

/**
 * A book found by `GET /api/search/books`. Each source fills in what it has, so every field can
 * be missing, and a few come as text from some sources and lists from others.
 */
@Serializable
data class BookMatchResult(
  val title: String? = null,
  val subtitle: String? = null,
  /** Names joined with commas, or a list */
  val author: JsonElement? = null,
  /** Names joined with commas, or a list */
  val narrator: JsonElement? = null,
  val publisher: String? = null,
  val publishedYear: String? = null,
  /** HTML from some sources, plain text from others */
  val description: String? = null,
  /** The cover image's URL, on the source's own host */
  val cover: String? = null,
  val isbn: String? = null,
  val asin: String? = null,
  /** A list, or names joined with commas */
  val genres: JsonElement? = null,
  /** A list, or names joined with commas */
  val tags: JsonElement? = null,
  val series: List<BookMatchSeries>? = null,
  val language: String? = null,
  /** In minutes */
  val duration: Double? = null,
  /** From 0 to 1, how well an Audible result matches the item it was searched for */
  val matchConfidence: Double? = null,
  val explicit: Boolean? = null,
  val abridged: Boolean? = null,
)

@Serializable
data class BookMatchSeries(
  val series: String? = null,
  val sequence: String? = null,
)

/** The body of `GET /api/search/covers`: the covers' addresses, on each source's own host */
@Serializable
data class CoverSearchResponse(
  val results: List<String> = emptyList(),
)
