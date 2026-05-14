package app.campfire.libraries.ui.detail.podcast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Session
import app.campfire.core.model.User
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.ui.detail.AbstractLibraryItemPresenter
import app.campfire.libraries.ui.detail.ContentUiState
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.slots.ChipsSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsTitle
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import app.campfire.libraries.ui.detail.composables.slots.CoverImageSlot
import app.campfire.libraries.ui.detail.composables.slots.EpisodeHeaderSlot
import app.campfire.libraries.ui.detail.composables.slots.EpisodeSlot
import app.campfire.libraries.ui.detail.composables.slots.SpacerSlot
import app.campfire.libraries.ui.detail.composables.slots.SplitAttributionSlot
import app.campfire.libraries.ui.detail.composables.slots.SummarySlot
import app.campfire.libraries.ui.detail.composables.slots.TitleSlot
import app.campfire.libraries.ui.detail.podcast.episode.showPodcastEpisodeBottomSheet
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.podcasts.api.screen.FindEpisodesScreen
import app.campfire.sessions.api.SessionsRepository
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.api.UserRepository
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.genres_title
import campfire.features.libraries.ui.generated.resources.tags_title
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class PodcastPresenter(
  private val analytics: Analytics,
  private val userRepository: UserRepository,
  private val sessionsRepository: SessionsRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackHistoryRepository: PlaybackHistoryRepository,
  private val playbackController: PlaybackController,
  private val addToPlaylistDialog: AddToPlaylistDialog,
) : AbstractLibraryItemPresenter {

  @Composable
  override fun present(
    screen: LibraryItemScreen,
    navigator: Navigator,
    libraryItem: LibraryItem,
  ): ContentUiState {
    val scope = rememberCoroutineScope()
    val overlayHost = LocalOverlayHost.current

    val currentUser by remember { userRepository.observeStatefulCurrentUser() }
      .collectAsState()

    val currentSession by remember {
      sessionsRepository.observeCurrentSession()
    }.collectAsState(null)

    val mediaProgress by remember {
      mediaProgressRepository.observeAllProgress()
        .map {
          it.associateBy { progress -> MediaProgressKey(progress) }
        }
    }.collectAsState(emptyMap())

    val slots = buildSlots(
      user = currentUser,
      libraryItem = libraryItem,
      currentSession = currentSession,
      mediaProgress = mediaProgress,
      addToPlaylistDialog = addToPlaylistDialog,
      sharedTransitionKey = screen.sharedTransitionKey,
    )

    // If we launch the screen with a targeted episode, open the drawer details
    var hasShownEpisode by rememberRetained { mutableStateOf(false) }
    val podcastEpisode by remember {
      derivedStateOf {
        val podcastMedia = libraryItem.media as Media.Podcast
        podcastMedia.episodes
          .find { it.id == screen.episodeId }
      }
    }

    LaunchedEffect(podcastEpisode) {
      if (podcastEpisode != null && !hasShownEpisode) {
        hasShownEpisode = true
        overlayHost.showPodcastEpisodeBottomSheet(libraryItem, podcastEpisode!!)
      }
    }

    return ContentUiState(
      slots = slots,
      eventSink = { event ->
        when (event) {
          is LibraryItemUiEvent.OpenEpisode -> {
            scope.launch {
              overlayHost.showPodcastEpisodeBottomSheet(libraryItem, event.episode)
            }
          }

          is LibraryItemUiEvent.PlayEpisodeClick -> {
            analytics.send(ActionEvent("play_item", Click))
            playbackController.startSession(
              itemId = libraryItem.id,
              episodeId = event.episode.id,
            )
          }

          is LibraryItemUiEvent.MarkEpisodeFinished -> {
            analytics.send(ActionEvent("mark_episode_finished", Click))
            val episode = event.episode

            // Only stop playback if this episode is the one currently playing.
            if (
              currentSession?.libraryItem?.id == episode.libraryItemId &&
              currentSession?.episodeId == episode.id
            ) {
              playbackController.stopSession(
                itemId = episode.libraryItemId,
                episodeId = episode.id,
              )
            }

            scope.launch {
              sessionsRepository.markDeleted(episode.libraryItemId, episode.id)
              mediaProgressRepository.markFinished(episode.libraryItemId, episode.id)
              playbackHistoryRepository.clear(episode.libraryItemId, episode.id)
            }
          }

          is LibraryItemUiEvent.MarkEpisodeNotFinished -> {
            analytics.send(ActionEvent("mark_episode_not_finished", Click))
            val episode = event.episode
            scope.launch {
              mediaProgressRepository.markNotFinished(episode.libraryItemId, episode.id)
            }
          }

          LibraryItemUiEvent.FindEpisodes -> {
            analytics.send(ActionEvent("find_episodes", Click))
            navigator.goTo(FindEpisodesScreen(libraryItem.id))
          }

          else -> Unit
        }
      },
    )
  }
}

@Composable
private fun buildSlots(
  user: User,
  libraryItem: LibraryItem,
  currentSession: Session?,
  mediaProgress: Map<MediaProgressKey, MediaProgress>,
  addToPlaylistDialog: AddToPlaylistDialog,
  sharedTransitionKey: String,
): List<ContentSlot> = buildList {
  this += CoverImageSlot(
    imageUrl = libraryItem.media.coverImageUrl,
    contentDescription = libraryItem.media.metadata.title,
    sharedTransitionKey = sharedTransitionKey,
  )

  this += TitleSlot(
    libraryItem = libraryItem,
    sharedTransitionKey = sharedTransitionKey,
  )

  this += SplitAttributionSlot(
    leftLabel = { "By" },
    leftAttributions = libraryItem.media.metadata.author?.let { listOf(it) } ?: emptyList(),
    rightLabel = { "Type" },
    rightAttributions = libraryItem.media.metadata.podcastType?.let { listOf(it) } ?: emptyList(),
  )

  val podcastMedia = libraryItem.media as Media.Podcast
  podcastMedia.metadata.description?.let { desc ->
    this += SpacerSlot.medium("summary_spacer")
    this += SummarySlot(
      description = desc,
      publisher = libraryItem.media.metadata.publisher,
      publishedYear = libraryItem.media.metadata.publishedYear,
    )
  }

  podcastMedia.metadata.genres.takeIf { it.isNotEmpty() }?.let { genres ->
    this += SpacerSlot.medium("genres_spacer")
    this += ChipsSlot(
      title = ChipsTitle(Res.plurals.genres_title, genres.size),
      chips = genres,
    )
  }

  podcastMedia.tags.takeIf { it.isNotEmpty() }?.let { tags ->
    this += SpacerSlot.medium("tags_spacer")
    this += ChipsSlot(
      title = ChipsTitle(Res.plurals.tags_title, tags.size),
      chips = tags,
    )
  }

  if (podcastMedia.episodes.isNotEmpty()) {
    this += SpacerSlot.large("chapters_spacer")
    this += EpisodeHeaderSlot(
      episodeCount = podcastMedia.episodes.count(),
      showFindEpisodes = user.type == User.Type.Admin ||
        user.type == User.Type.Root,
    )

    this += podcastMedia.episodes.map { episode ->
      val progress = mediaProgress[MediaProgressKey(episode.libraryItemId, episode.id)]
      EpisodeSlot(
        libraryItem = libraryItem,
        media = podcastMedia,
        episode = episode,
        progress = progress,
        isCurrentSession = currentSession?.libraryItem?.id == episode.libraryItemId &&
          currentSession.episodeId == episode.id,
        addToPlaylistDialog = addToPlaylistDialog,
      )
    }
  }
}
