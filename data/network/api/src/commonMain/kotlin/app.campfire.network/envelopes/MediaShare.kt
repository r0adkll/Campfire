package app.campfire.network.envelopes

import app.campfire.network.models.MediaType
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class MediaShareRequest(
  val slug: String,
  val expiresAt: Long,
  val mediaItemId: String,
  val mediaItemType: MediaType,
  val isDownloadable: Boolean,
)

@Serializable
data class MediaShareResponse(
  val id: String,
  val mediaItemId: String,
  val mediaItemType: MediaType,
  val slug: String,
  val expiresAt: LocalDateTime,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val isDownloadable: Boolean,
)
