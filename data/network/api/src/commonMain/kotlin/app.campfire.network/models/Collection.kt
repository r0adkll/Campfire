package app.campfire.network.models

import app.campfire.network.envelopes.Envelope
import kotlinx.serialization.Serializable

@Serializable
data class Collection(
  val id: String,
  val libraryId: String,
  val name: String,
  val description: String?,
  val cover: String? = null,
  val coverFullPath: String? = null,
  val books: List<LibraryItemExpanded>,
  val lastUpdate: Long,
  val createdAt: Long,
) : Envelope() {
  override fun applyPostage() {
    books.forEach { it.applyOrigin(origin) }
  }
}
