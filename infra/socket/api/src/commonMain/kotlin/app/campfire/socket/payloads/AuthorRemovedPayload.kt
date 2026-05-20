package app.campfire.socket.payloads

import kotlinx.serialization.Serializable

@Serializable
data class AuthorRemovedPayload(
  val id: String,
  val libraryId: String,
)
