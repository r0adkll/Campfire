package app.campfire.sessions.ui.expanded

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.audioplayer.PlaybackController
import app.campfire.sessions.api.SessionQueue
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

typealias ExpandedPlaybackPresenterFactory = () -> ExpandedPlaybackPresenter

@Inject
class ExpandedPlaybackPresenter(
  private val sessionQueue: SessionQueue,
  private val playbackController: PlaybackController,
) : Presenter<ExpandedPlaybackUiState> {

  @Composable
  override fun present(): ExpandedPlaybackUiState {
    val scope = rememberCoroutineScope()

    val queue by remember {
      sessionQueue.observeAll()
    }.collectAsState(emptyList())

    return ExpandedPlaybackUiState(
      queue = queue,
      reorderSink = { from, to ->
        sessionQueue.reorder(from, to)
      },
    ) { event ->
      when (event) {
        is ExpandedPlaybackUiEvent.QueueItemClick -> {
          playbackController.startSession(event.item.id)
          scope.launch {
            sessionQueue.remove(event.item)
          }
        }

        is ExpandedPlaybackUiEvent.RemoveQueueItem -> {
          scope.launch {
            sessionQueue.remove(event.item)
          }
        }
      }
    }
  }
}
