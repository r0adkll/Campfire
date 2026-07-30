// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A single library item with full media details. Sealed: kotlinx-serialization dispatches to
 * [Book] or [Podcast] based on the JSON's `mediaType` field. The `/api/items/:id?expanded=1`
 * endpoint returns either shape — callers `when` on the subtype.
 *
 * Book-only contexts (Collection.books, SearchResult.books) should declare the field type as
 * [Book] directly to skip dispatch and stay strongly typed.
 */
@Serializable(with = LibraryItemExpandedSerializer::class)
sealed class LibraryItemExpanded : LibraryItemBase() {
  abstract val libraryFiles: List<LibraryFile>
  abstract val userMediaProgress: MediaProgress?

  @Serializable
  data class Book(
    override val id: String,
    override val ino: String,
    override val libraryId: String,
    override val oldLibraryItemId: String? = null,
    override val folderId: String,
    override val path: String,
    override val relPath: String,
    override val isFile: Boolean,
    override val mtimeMs: Long,
    override val ctimeMs: Long,
    override val birthtimeMs: Long,
    override val addedAt: Long,
    override val updatedAt: Long,
    override val isMissing: Boolean,
    override val isInvalid: Boolean,
    override val mediaType: MediaType,
    // This shouldn't be nullable, but sometimes the API (i.e. Search) returns unexpected values
    override val size: Long? = null,
    override val numFiles: Int? = null,
    val media: MediaExpanded,
    override val libraryFiles: List<LibraryFile>,
    override val userMediaProgress: MediaProgress? = null,
  ) : LibraryItemExpanded()

  @Serializable
  data class Podcast(
    override val id: String,
    override val ino: String,
    override val libraryId: String,
    override val oldLibraryItemId: String? = null,
    override val folderId: String,
    override val path: String,
    override val relPath: String,
    override val isFile: Boolean,
    override val mtimeMs: Long,
    override val ctimeMs: Long,
    override val birthtimeMs: Long,
    override val addedAt: Long,
    override val updatedAt: Long,
    override val isMissing: Boolean,
    override val isInvalid: Boolean,
    override val mediaType: MediaType,
    override val size: Long? = null,
    override val numFiles: Int? = null,
    val media: app.campfire.network.models.Podcast,
    override val libraryFiles: List<LibraryFile> = emptyList(),
    override val userMediaProgress: MediaProgress? = null,
  ) : LibraryItemExpanded()
}

internal object LibraryItemExpandedSerializer :
  JsonContentPolymorphicSerializer<LibraryItemExpanded>(LibraryItemExpanded::class) {
  override fun selectDeserializer(
    element: JsonElement,
  ): DeserializationStrategy<LibraryItemExpanded> {
    val mediaType = element.jsonObject["mediaType"]?.jsonPrimitive?.content
    return when (mediaType) {
      "podcast", "podcastEpisode" -> LibraryItemExpanded.Podcast.serializer()
      else -> LibraryItemExpanded.Book.serializer()
    }
  }
}
