package app.campfire.libraries.ui.detail.podcast.episode

import androidx.compose.runtime.Composable
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.PlaybackController
import app.campfire.core.model.PodcastEpisode
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias PodcastEpisodePresenterFactory = (PodcastEpisode, OverlayNavigator<Unit>) -> PodcastEpisodePresenter

@Inject
class PodcastEpisodePresenter(
  @Assisted private val episode: PodcastEpisode,
  @Assisted private val navigator: OverlayNavigator<Unit>,
  private val analytics: Analytics,
  private val playbackController: PlaybackController,
) : Presenter<PodcastEpisodeUiState> {

  @Composable
  override fun present(): PodcastEpisodeUiState {
    return PodcastEpisodeUiState(
      episode = episode,
    ) { event ->
      when (event) {
        PodcastEpisodeUiEvent.PlayClick -> {
          analytics.send(ActionEvent("play_item", Click))
          playbackController.startSession(
            itemId = episode.libraryItemId,
            episodeId = episode.id,
          )
          navigator.finish(Unit)
        }
      }
    }
  }
}
