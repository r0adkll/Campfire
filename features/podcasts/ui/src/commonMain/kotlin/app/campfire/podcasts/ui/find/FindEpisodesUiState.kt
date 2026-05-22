package app.campfire.podcasts.ui.find

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import app.campfire.podcasts.api.RemotePodcastEpisode
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class FindEpisodesUiState(
  val textFieldState: TextFieldState,
  val feedState: FeedState,
  val selectedEnclosureUrls: Set<String>,
  val isQueuingDownload: Boolean,
  val canBrowseFeed: Boolean,
  val eventSink: (FindEpisodesUiEvent) -> Unit,
) : CircuitUiState

sealed interface FeedState {
  data object Loading : FeedState
  data object Forbidden : FeedState
  data object Error : FeedState
  data object NoFeedUrl : FeedState
  data class Loaded(val episodes: List<FeedEpisodeRow>) : FeedState
}

@Immutable
data class FeedEpisodeRow(
  val episode: RemotePodcastEpisode,
  val downloadState: DownloadState,
)

/**
 * Composite state for a single feed row, derived in [FindEpisodesPresenter] by merging:
 * - the local library's episode set ([InLibrary])
 * - the server's live download queue ([Queued], [Downloading])
 * - the tracker's recently-finished URL set ([Finished])
 * - the local "user-just-queued" session set ([Queued] also)
 *
 * Precedence: an URL in the library always wins. After that, [Downloading] > [Queued] >
 * [Finished] > [Available].
 */
sealed interface DownloadState {
  /** Available to queue — not in local library and not in flight. */
  data object Available : DownloadState

  /** Sitting in the server's download queue. */
  data object Queued : DownloadState

  /** Server is actively pulling the file. */
  data object Downloading : DownloadState

  /**
   * Transient — server just told us the download finished successfully. Fades to [InLibrary]
   * once the new episode lands in the local DB (or expires after the tracker's TTL).
   */
  data object Finished : DownloadState

  /** Already in the local library (was there when the screen opened, or has since arrived). */
  data object InLibrary : DownloadState
}

sealed interface FindEpisodesUiEvent : CircuitUiEvent {
  data object Back : FindEpisodesUiEvent
  data class ToggleSelection(val episode: RemotePodcastEpisode) : FindEpisodesUiEvent
  data object ClearSelection : FindEpisodesUiEvent
  data object DownloadSelected : FindEpisodesUiEvent
  data object Retry : FindEpisodesUiEvent
}
