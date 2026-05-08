package app.campfire.libraries.ui.detail.podcast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.PlaybackController
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
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
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.genres_title
import campfire.features.libraries.ui.generated.resources.tags_title
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class PodcastPresenter(
  private val analytics: Analytics,
  private val playbackController: PlaybackController,
) : AbstractLibraryItemPresenter {

  @Composable
  override fun present(
    screen: LibraryItemScreen,
    navigator: Navigator,
    libraryItem: LibraryItem,
  ): ContentUiState {
    val scope = rememberCoroutineScope()
    val overlayHost = LocalOverlayHost.current

    val slots = buildSlots(
      libraryItem = libraryItem,
      sharedTransitionKey = screen.sharedTransitionKey,
    )

    // If we launch the screen with a targeted episode, open the drawer details
    var hasShownEpisode by rememberRetained { mutableStateOf(false) }
    LaunchedEffect(libraryItem) {
      val podcastMedia = libraryItem.media as Media.Podcast
      val podcastEpisode = podcastMedia.episodes.find { it.id == screen.episodeId }
      if (podcastEpisode != null && !hasShownEpisode) {
        hasShownEpisode = true
        overlayHost.showPodcastEpisodeBottomSheet(podcastEpisode)
      }
    }

    return ContentUiState(
      slots = slots,
      eventSink = { event ->
        when (event) {
          is LibraryItemUiEvent.OpenEpisode -> {
            scope.launch {
              overlayHost.showPodcastEpisodeBottomSheet(event.episode)
            }
          }

          is LibraryItemUiEvent.PlayEpisodeClick -> {
            analytics.send(ActionEvent("play_item", Click))
            playbackController.startSession(
              itemId = libraryItem.id,
              episodeId = event.episode.id,
            )
          }

          else -> Unit
        }
      },
    )
  }
}

@Composable
private fun buildSlots(
  libraryItem: LibraryItem,
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
    this += EpisodeHeaderSlot()

    this += podcastMedia.episodes.map { episode ->
      EpisodeSlot(podcastMedia, episode)
    }
  }
}
