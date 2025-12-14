package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class AudioBookmark(
  val libraryItemId: String,
  val title: String,
  val time: Float,
  val createdAt: Long,
)
