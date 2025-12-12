package app.campfire.core.model

typealias MediaProgressId = String

data class MediaProgress(
  val id: MediaProgressId,
  val userId: String,
  val libraryItemId: String,
  val episodeId: String? = null,
  val mediaItemId: String,
  val mediaItemType: MediaType,
  val duration: Float?,
  val progress: Float,
  val currentTime: Float,
  val isFinished: Boolean,
  val hideFromContinueListening: Boolean,
  val ebookLocation: String? = null,
  val ebookProgress: Float? = null,
  val lastUpdate: Long,
  val startedAt: Long,
  val finishedAt: Long? = null,
) {

  val actualProgress: Float
    get() = if (isFinished) {
      1f
    } else duration?.let {
      currentTime / duration
    } ?: progress

  companion object {
    const val UNKNOWN_ID = "unknown_id"
  }
}
