package app.campfire.podcasts.ui.downloads

import app.campfire.core.model.LibraryItemId
import app.campfire.podcasts.api.RemoteEpisodeDownload
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.ImmutableList

data class PodcastDownloadQueueUiState(
  val groups: ImmutableList<DownloadGroup>,
  val isAdmin: Boolean,
  val isRefreshing: Boolean,
  val eventSink: (PodcastDownloadQueueUiEvent) -> Unit,
) : CircuitUiState

/**
 * All in-flight + queued downloads grouped under their parent podcast.
 */
data class DownloadGroup(
  val libraryItemId: LibraryItemId,
  val podcastTitle: String,
  val downloads: ImmutableList<RemoteEpisodeDownload>,
)

sealed interface PodcastDownloadQueueUiEvent : CircuitUiEvent {
  data object Refresh : PodcastDownloadQueueUiEvent
  data class ClearQueue(val libraryItemId: LibraryItemId) : PodcastDownloadQueueUiEvent
  data class OpenPodcast(val libraryItemId: LibraryItemId) : PodcastDownloadQueueUiEvent
}
