package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * Folder used in library
 *
 * @param id The ID of the folder.
 * @param fullPath The path on the server for the folder. (Read Only)
 * @param libraryId The ID of the library.
 * @param addedAt The time (in ms since POSIX epoch) when added to the server.
 */
@Serializable
data class Folder(
  val id: String,
  val fullPath: String,
  val libraryId: String,
  val addedAt: Long,
)
