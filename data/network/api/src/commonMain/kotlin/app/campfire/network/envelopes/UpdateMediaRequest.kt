// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

/**
 * The body of `PATCH /api/items/:id/media`. The server only changes what's sent, so every field
 * defaults to null and is left out: a partial update.
 */
@Serializable
data class UpdateMediaRequest(
  val metadata: UpdateBookMetadataRequest? = null,
  /** Tags sit beside the metadata, not in it */
  val tags: List<String>? = null,
)

/**
 * A book's metadata to change. The server stores an empty string as no value, which is how a
 * text field is cleared.
 */
@Serializable
data class UpdateBookMetadataRequest(
  val title: String? = null,
  val subtitle: String? = null,
  val description: String? = null,
  val publisher: String? = null,
  val publishedYear: String? = null,
  val language: String? = null,
  val isbn: String? = null,
  val asin: String? = null,
  val explicit: Boolean? = null,
  val abridged: Boolean? = null,
  /** The whole list of authors, by name; the server links or creates each */
  val authors: List<AuthorNameRequest>? = null,
  val narrators: List<String>? = null,
  val genres: List<String>? = null,
  /** The whole list of series the book is in */
  val series: List<SeriesSequenceRequest>? = null,
)

@Serializable
data class AuthorNameRequest(val name: String)

/** A series by name, with the book's place in it, which the server takes as text */
@Serializable
data class SeriesSequenceRequest(
  val name: String,
  val sequence: String? = null,
)
