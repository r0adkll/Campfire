package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

@Serializable
data class CreateBookmarkRequest(
  val time: Int,
  val title: String,
)
