package app.campfire.socket.payloads

import kotlinx.serialization.Serializable

@Serializable
data class ItemRemovedPayload(
  val id: String,
  val libraryId: String,
)
