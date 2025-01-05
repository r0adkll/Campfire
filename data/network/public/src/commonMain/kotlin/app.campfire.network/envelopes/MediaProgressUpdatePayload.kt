package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

@Serializable
data class MediaProgressUpdatePayload(
  val libraryItemId: String? = null,
  val episodeId: String? = null,
  val duration: Float,
  val progress: Float,
  val currentTime: Float,
  val isFinished: Boolean? = null,
  val hideFromContinueListening: Boolean,
  val finishedAt: Long? = null,
  val startedAt: Long,
)
