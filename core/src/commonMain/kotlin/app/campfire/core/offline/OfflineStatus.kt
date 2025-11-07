package app.campfire.core.offline

sealed interface OfflineStatus {
  data object None : OfflineStatus
  data object Queued : OfflineStatus
  data class Downloading(val progress: Float) : OfflineStatus
  data object Available : OfflineStatus
  data object Failed : OfflineStatus
}
