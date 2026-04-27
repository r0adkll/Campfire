package app.campfire.network.models

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A single item on the server, like a book or podcast. Minified media format.
 *
 * Sealed: kotlinx-serialization dispatches to [Book] or [Podcast] based on the JSON's
 * `mediaType` field. Endpoints like `/api/libraries/:id/items` return a mix of either
 * shape depending on the parent library's mediaType.
 *
 * Book-only contexts (Author.libraryItems, Series.books, Collection.books, etc.) should
 * declare the field type as [Book] directly to skip dispatch and stay strongly typed.
 *
 * @param id The ID of library items after 2.3.0.
 * @param oldLibraryItemId The ID of library items on server version 2.2.23 and before.
 * @param ino The inode of the item in the file system.
 * @param libraryId The ID of the library.
 * @param folderId The ID of the folder.
 * @param path The path of the library item on the server.
 * @param relPath The path, relative to the library folder, of the library item.
 * @param isFile Whether the library item is a single file in the root of the library folder.
 * @param mtimeMs The time (in ms since POSIX epoch) when the library item was last modified on disk.
 * @param ctimeMs The time (in ms since POSIX epoch) when the library item status was changed on disk.
 * @param birthtimeMs The time (in ms since POSIX epoch) when the library item was created on disk. Will be 0 if unknown.
 * @param addedAt The time (in ms since POSIX epoch) when added to the server.
 * @param updatedAt The time (in ms since POSIX epoch) when last updated.
 * @param isMissing Whether the library item was scanned and no longer exists.
 * @param isInvalid Whether the library item was scanned and no longer has media files.
 * @param mediaType
 */
@Serializable(with = LibraryItemMinifiedSerializer::class)
sealed class LibraryItemMinified : LibraryItemBase() {
  abstract val libraryFiles: List<LibraryFile>?

  /**
   * When returned from the personalized library endpoint, and its shelf has an id of
   * `recommended` then this field will be non-null.
   *
   * The recommendation weight of the library item.
   */
  abstract val weight: Double?

  /**
   * When returned from the personalized library endpoint, and its shelf has an id of
   * `continue-listening` then this field will be non-null.
   */
  abstract val progressLastUpdate: Long?

  /**
   * When returned from the personalized library endpoint, and its shelf has an id of
   * `listen-again` then this field will be non-null.
   */
  abstract val finishedAt: Long?

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
    override val numFiles: Int? = null,
    override val size: Long,
    val media: MediaMinified,
    override val libraryFiles: List<LibraryFile>? = null,
    override val weight: Double? = null,
    override val progressLastUpdate: Long? = null,
    override val finishedAt: Long? = null,

    /**
     * When returned from the personalized library endpoint, and its shelf has an id of
     * `continue-series` then this field will be non-null.
     *
     * The time (in ms since POSIX epoch) of the most recent progress update of any book in the series
     */
    val prevBookInProgressLastUpdate: Long? = null,
  ) : LibraryItemMinified()

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
    override val numFiles: Int? = null,
    override val size: Long,
    val media: app.campfire.network.models.Podcast,
    override val libraryFiles: List<LibraryFile>? = null,
    override val weight: Double? = null,
    override val progressLastUpdate: Long? = null,
    override val finishedAt: Long? = null,

    /**
     * Populated only when this item is returned from a personalized shelf with id
     * `episodes-recently-added`, `continue-listening`, or `listen-again` — the specific
     * episode the shelf is highlighting.
     */
    val recentEpisode: PodcastEpisode? = null,
  ) : LibraryItemMinified()
}

internal object LibraryItemMinifiedSerializer :
  JsonContentPolymorphicSerializer<LibraryItemMinified>(LibraryItemMinified::class) {
  override fun selectDeserializer(
    element: JsonElement,
  ): DeserializationStrategy<LibraryItemMinified> {
    val mediaType = element.jsonObject["mediaType"]?.jsonPrimitive?.content
    return when (mediaType) {
      "podcast", "podcastEpisode" -> LibraryItemMinified.Podcast.serializer()
      else -> LibraryItemMinified.Book.serializer()
    }
  }
}
