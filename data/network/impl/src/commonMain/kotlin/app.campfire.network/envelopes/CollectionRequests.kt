package app.campfire.network.envelopes

import app.campfire.core.model.LibraryId
import kotlinx.serialization.Serializable

@Serializable
class NewCollectionRequest(
  val libraryId: LibraryId,
  val name: String,
  val books: List<String>,
  val description: String? = null,
)

@Serializable
class UpdateCollectionRequest(
  val name: String? = null,
  val description: String? = null,
  val books: List<String>? = null,
)

@Serializable
class AddBookToCollectionRequest(
  val id: String,
)
