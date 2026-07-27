package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * The ebook file attached to a book library item. Only serialized on the basic and
 * expanded media shapes — the minified shape flattens it to `ebookFormat`.
 *
 * @param ino The inode of the file.
 * @param metadata The file's metadata.
 * @param ebookFormat The format of the ebook, e.g. "epub", "pdf".
 * @param addedAt The time (in ms since POSIX epoch) when the file was added.
 * @param updatedAt The time (in ms since POSIX epoch) when the file was last updated.
 */
@Serializable
data class EBookFile(
  val ino: String,
  val metadata: FileMetadata,
  val ebookFormat: String? = null,
  val addedAt: Long = 0L,
  val updatedAt: Long = 0L,
)
