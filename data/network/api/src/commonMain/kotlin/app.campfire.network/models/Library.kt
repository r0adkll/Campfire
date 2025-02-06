package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * A library object which includes either books or podcasts.
 *
 * @param id The ID of the library.
 * @param name The name of the library.
 * @param folders The folders that belong to the library.
 * @param displayOrder Display position of the library in the list of libraries. Must be >= 1.
 * @param icon The selected icon for the library. See Library Icons for a list of possible icons.
 * @param mediaType The type of media that the library contains. Will be `book` or `podcast`. (Read Only)
 * @param provider Preferred metadata provider for the library. See Metadata Providers for a list of possible providers.
 * @param settings
 * @param createdAt The time (in ms since POSIX epoch) when was created.
 * @param lastUpdate The time (in ms since POSIX epoch) when last updated.
 */
@Serializable
data class Library(
  val id: String,
  val name: String,
  val folders: List<Folder>,
  val displayOrder: Int,
  val icon: String,
  val mediaType: String,
  val provider: String,
  val settings: LibrarySettings,
  val createdAt: Long,
  val lastUpdate: Long,
) : NetworkModel()
