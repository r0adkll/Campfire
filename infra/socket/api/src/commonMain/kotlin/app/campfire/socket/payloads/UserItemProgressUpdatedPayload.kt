package app.campfire.socket.payloads

import app.campfire.network.models.MediaProgress
import kotlinx.serialization.Serializable

@Serializable
data class UserItemProgressUpdatedPayload(
  val id: String,
  val data: MediaProgress,
)
