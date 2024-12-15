package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

@Serializable
data class MediaProgressUpdatePayload(
  val duration: Float,
  val progress: Float,
  val currentTime: Float,
  val isFinished: Boolean,
  val hideFromContinueListening: Boolean,
  val finishedAt: Long? = null,
  val startedAt: Long,
)
