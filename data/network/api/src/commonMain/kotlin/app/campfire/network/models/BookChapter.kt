package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * A book chapter. Includes the title and timestamps.
 *
 * @param id The ID of the book chapter.
 * @param start When in the book (in seconds) the chapter starts.
 * @param end When in the book (in seconds) the chapter ends.
 * @param title The title of the chapter.
 */
@Serializable
data class BookChapter(
  val id: Int,
  val start: Float,
  val end: Float,
  val title: String,
)
