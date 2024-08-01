package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class MediaProgress(
  val id: String,
  val libraryItemId: String,
  val episodeId: String? = null,
  val duration: Float,
  val progress: Float,
  val currentTime: Float,
  val isFinished: Boolean,
  val hideFromContinueListening: Boolean,
  val lastUpdate: Long,
  val startedAt: Long,
  val finishedAt: Long? = null,
)
