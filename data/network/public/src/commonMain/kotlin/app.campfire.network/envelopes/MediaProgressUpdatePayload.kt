package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

@Serializable
data class MediaProgressUpdatePayload(
  val libraryItemId: String? = null,
  val episodeId: String? = null,
  val duration: Float? = null,
  val progress: Float? = null,
  val currentTime: Float? = null,
  val isFinished: Boolean? = null,
  val hideFromContinueListening: Boolean? = null,
  val finishedAt: Long? = null,
  val startedAt: Long? = null,
)
