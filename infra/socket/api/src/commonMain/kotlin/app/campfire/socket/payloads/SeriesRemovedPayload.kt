package app.campfire.socket.payloads

import kotlinx.serialization.Serializable

@Serializable
data class SeriesRemovedPayload(
  val id: String,
  val libraryId: String,
)
