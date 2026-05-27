package app.campfire.podcasts.ui.find

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.common.compose.util.rememberRetainedCoroutineScope
import app.campfire.core.di.UserScope
import app.campfire.core.model.Media
import app.campfire.core.model.User
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.RemoteEpisodeDownloadTracker
import app.campfire.podcasts.api.RemotePodcastEpisode
import app.campfire.podcasts.api.screen.FindEpisodesScreen
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import kotlin.collections.orEmpty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(FindEpisodesScreen::class, UserScope::class)
@Inject
class FindEpisodesPresenter(
  @Assisted private val screen: FindEpisodesScreen,
  @Assisted private val navigator: Navigator,
  private val analytics: Analytics,
  private val libraryItemRepository: LibraryItemRepository,
  private val podcastsRepository: PodcastsRepository,
  private val userRepository: UserRepository,
  private val downloadTracker: RemoteEpisodeDownloadTracker,
) : NonPausablePresenter<FindEpisodesUiState> {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): FindEpisodesUiState {
    val scope = rememberRetainedCoroutineScope()
    val textFieldState = rememberRetained { TextFieldState() }

    var retryCount by remember { mutableStateOf(0) }
    var selectedEnclosureUrls by remember { mutableStateOf(emptySet<String>()) }
    var queuedEnclosureUrls by remember { mutableStateOf(emptySet<String>()) }
    var isQueuingDownload by remember { mutableStateOf(false) }

    val currentUser by userRepository.userFlow.collectAsState()
    val canBrowseFeed = currentUser.type.canBrowseFeed

    val libraryItem by remember {
      libraryItemRepository.observeLibraryItem(screen.libraryItemId)
    }.collectAsState(null)

    val feedLoadResult by remember(retryCount) {
      snapshotFlow { libraryItem }
        .mapLatest { item ->
          if (item == null) return@mapLatest FeedLoadResult.Loading

          val podcastMedia = item.media as Media.Podcast
          val feedUrl = podcastMedia.metadata.feedUrl
            ?: return@mapLatest FeedLoadResult.NoFeedUrl

          val result = podcastsRepository.fetchPodcastFeedDetails(feedUrl)
          result.fold(
            onSuccess = { FeedLoadResult.Success(it.episodes) },
            onFailure = { FeedLoadResult.Error },
          )
        }
    }.collectAsState(FeedLoadResult.Loading)

    val serverDownloads by remember(screen.libraryItemId) {
      downloadTracker.observe(screen.libraryItemId)
    }.collectAsState(emptyList())
    val recentlyFinishedUrls by downloadTracker.recentlyFinishedUrls.collectAsState()

    val feedState: FeedState by remember {
      snapshotFlow {
        val query = textFieldState.text.toString()

        val podcastMedia = libraryItem?.media as? Media.Podcast
        val downloadedKeys = podcastMedia?.episodes?.mapTo(mutableSetOf()) {
          DownloadedKey(it.title, it.publishedAtMillis)
        }.orEmpty()

        val serverDownloadByUrl = serverDownloads.associateBy { it.url }

        when (val load = feedLoadResult) {
          FeedLoadResult.Loading -> FeedState.Loading
          FeedLoadResult.Forbidden -> FeedState.Forbidden
          FeedLoadResult.NoFeedUrl -> FeedState.NoFeedUrl
          FeedLoadResult.Error -> FeedState.Error
          is FeedLoadResult.Success -> {
            val filtered = load.episodes.filter { it.matches(query) }
            val sorted = filtered.sortedByDescending { it.publishedAtMillis ?: Long.MIN_VALUE }
            FeedState.Loaded(
              episodes = sorted.map { episode ->
                FeedEpisodeRow(
                  episode = episode,
                  downloadState = deriveDownloadState(
                    episode = episode,
                    inLibrary = DownloadedKey(episode.title, episode.publishedAtMillis) in downloadedKeys,
                    serverDownload = serverDownloadByUrl[episode.enclosureUrl],
                    recentlyFinished = episode.enclosureUrl in recentlyFinishedUrls,
                    localQueued = episode.enclosureUrl in queuedEnclosureUrls,
                  ),
                )
              },
            )
          }
        }
      }
    }.collectAsState(FeedState.Loading)

    return FindEpisodesUiState(
      textFieldState = textFieldState,
      feedState = feedState,
      selectedEnclosureUrls = selectedEnclosureUrls,
      isQueuingDownload = isQueuingDownload,
      canBrowseFeed = canBrowseFeed,
      eventSink = { event ->
        when (event) {
          FindEpisodesUiEvent.Back -> navigator.pop()

          is FindEpisodesUiEvent.ToggleSelection -> {
            val url = event.episode.enclosureUrl
            selectedEnclosureUrls = if (url in selectedEnclosureUrls) {
              selectedEnclosureUrls - url
            } else {
              selectedEnclosureUrls + url
            }
          }

          FindEpisodesUiEvent.ClearSelection -> {
            selectedEnclosureUrls = emptySet()
          }

          FindEpisodesUiEvent.Retry -> {
            retryCount++
          }

          FindEpisodesUiEvent.DownloadSelected -> {
            val loaded = feedState as? FeedState.Loaded ?: return@FindEpisodesUiState
            val toDownload = loaded.episodes
              .filter {
                it.episode.enclosureUrl in selectedEnclosureUrls &&
                  it.downloadState is DownloadState.Available
              }
              .map { it.episode }
            if (toDownload.isEmpty()) return@FindEpisodesUiState
            analytics.send(ActionEvent("podcast_episode_queue_downloads", Click))
            isQueuingDownload = true
            scope.launch {
              val result = podcastsRepository.queueEpisodeDownloads(
                libraryItemId = screen.libraryItemId,
                episodes = toDownload,
              )
              isQueuingDownload = false
              if (result.isSuccess) {
                queuedEnclosureUrls = queuedEnclosureUrls +
                  toDownload.map { it.enclosureUrl }
                selectedEnclosureUrls = emptySet()
              }
            }
          }
        }
      },
    )
  }
}

private data class DownloadedKey(
  val title: String,
  val publishedAtMillis: Long?,
)

/**
 * Merge precedence:
 *   InLibrary > Downloading > Queued > Finished > Available
 *
 * `InLibrary` wins regardless of tracker state — if the file is on disk, the row should reflect
 * that, even if a stale tracker entry hasn't been pruned yet.
 */
private fun deriveDownloadState(
  episode: RemotePodcastEpisode,
  inLibrary: Boolean,
  serverDownload: RemoteEpisodeDownload?,
  recentlyFinished: Boolean,
  localQueued: Boolean,
): DownloadState {
  if (inLibrary) return DownloadState.InLibrary
  return when (serverDownload?.state) {
    RemoteEpisodeDownload.State.Downloading -> DownloadState.Downloading
    RemoteEpisodeDownload.State.Queued -> DownloadState.Queued
    null -> when {
      localQueued -> DownloadState.Queued
      recentlyFinished -> DownloadState.Finished
      else -> DownloadState.Available
    }
  }
}

private sealed interface FeedLoadResult {
  data object Loading : FeedLoadResult
  data object Forbidden : FeedLoadResult
  data object NoFeedUrl : FeedLoadResult
  data object Error : FeedLoadResult
  data class Success(val episodes: List<RemotePodcastEpisode>) : FeedLoadResult
}

private fun RemotePodcastEpisode.matches(query: String): Boolean {
  if (query.isBlank()) return true
  val q = query.trim()
  return title.contains(q, ignoreCase = true) ||
    (subtitle?.contains(q, ignoreCase = true) == true) ||
    (descriptionPlain?.contains(q, ignoreCase = true) == true) ||
    (description?.contains(q, ignoreCase = true) == true)
}

private val User.Type.canBrowseFeed: Boolean
  get() = this == User.Type.Admin || this == User.Type.Root
