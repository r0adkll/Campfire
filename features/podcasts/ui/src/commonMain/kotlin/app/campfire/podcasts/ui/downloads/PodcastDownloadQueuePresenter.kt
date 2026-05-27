package app.campfire.podcasts.ui.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.core.di.UserScope
import app.campfire.core.model.User
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.RemoteEpisodeDownloadTracker
import app.campfire.podcasts.api.screen.PodcastDownloadQueueScreen
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(PodcastDownloadQueueScreen::class, UserScope::class)
@Inject
class PodcastDownloadQueuePresenter(
  @Suppress("unused") @Assisted private val navigator: Navigator,
  private val userRepository: UserRepository,
  private val podcastsRepository: PodcastsRepository,
  private val tracker: RemoteEpisodeDownloadTracker,
) : NonPausablePresenter<PodcastDownloadQueueUiState> {

  @Composable
  override fun present(): PodcastDownloadQueueUiState {
    val scope = rememberCoroutineScope()
    val currentUser by userRepository.userFlow.collectAsState()
    val downloads by tracker.state.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Hydrate when the screen first lands or the user switches libraries.
    LaunchedEffect(currentUser.selectedLibraryId) {
      isRefreshing = true
      podcastsRepository.fetchEpisodeDownloads(currentUser.selectedLibraryId)
      isRefreshing = false
    }

    val groups = remember(downloads) {
      downloads
        .map { (libraryItemId, items) ->
          DownloadGroup(
            libraryItemId = libraryItemId,
            podcastTitle = items.firstNotNullOfOrNull { it.podcastTitle }
              ?: libraryItemId,
            downloads = items.sortedDownloads().toImmutableList(),
          )
        }
        .sortedBy { it.podcastTitle.lowercase() }
        .toImmutableList()
    }

    return PodcastDownloadQueueUiState(
      groups = groups,
      isAdmin = currentUser.type == User.Type.Admin || currentUser.type == User.Type.Root,
      isRefreshing = isRefreshing,
      eventSink = { event ->
        when (event) {
          is PodcastDownloadQueueUiEvent.Refresh -> scope.launch {
            isRefreshing = true
            podcastsRepository.fetchEpisodeDownloads(currentUser.selectedLibraryId)
            isRefreshing = false
          }
          is PodcastDownloadQueueUiEvent.ClearQueue -> scope.launch {
            podcastsRepository.clearEpisodeDownloadQueue(event.libraryItemId)
          }
          is PodcastDownloadQueueUiEvent.OpenPodcast -> navigator.goTo(
            LibraryItemScreen(libraryItemId = event.libraryItemId),
          )
        }
      },
    )
  }

  /** Downloading first, then queued by createdAt (oldest first), keeping server order. */
  private fun List<RemoteEpisodeDownload>.sortedDownloads(): List<RemoteEpisodeDownload> {
    return sortedWith(
      compareBy(
        { it.state.ordinal == RemoteEpisodeDownload.State.Queued.ordinal },
        { it.createdAt ?: Long.MAX_VALUE },
      ),
    )
  }
}
