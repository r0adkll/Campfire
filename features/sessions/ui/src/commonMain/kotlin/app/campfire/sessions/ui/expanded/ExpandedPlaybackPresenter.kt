package app.campfire.sessions.ui.expanded

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import app.campfire.audioplayer.PlaybackController
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.libraries.api.LibraryItemValidator
import app.campfire.sessions.api.SessionQueue
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias ExpandedPlaybackPresenterFactory = (LibraryItem) -> ExpandedPlaybackPresenter

@Inject
class ExpandedPlaybackPresenter(
  @Assisted private val libraryItem: LibraryItem,
  private val sessionQueue: SessionQueue,
  private val playbackController: PlaybackController,
  private val libraryItemValidator: LibraryItemValidator,
) : Presenter<ExpandedPlaybackUiState> {

  @Composable
  override fun present(): ExpandedPlaybackUiState {
    val scope = rememberCoroutineScope()

    val queue by remember {
      sessionQueue.observeAll()
    }.collectAsState(emptyList())

    val localQueue = remember(queue) {
      queue.toMutableStateList()
    }

    val itemValidation by remember {
      flow {
        val validation = libraryItemValidator.validate(libraryItem)
        emit(validation)
      }
    }.collectAsState(LibraryItemValidation.Success)

    return ExpandedPlaybackUiState(
      validation = itemValidation,
      queue = localQueue,
      reorderSink = { from, to ->
        val fromIndex = localQueue.indexOfFirst { it.id == from }
        val toIndex = localQueue.indexOfFirst { it.id == to }
        localQueue.add(toIndex, localQueue.removeAt(fromIndex))
      },
    ) { event ->
      when (event) {
        ExpandedPlaybackUiEvent.ReorderStopped -> {
          scope.launch {
            sessionQueue.reorder(localQueue)
          }
        }

        ExpandedPlaybackUiEvent.ClearQueue -> {
          scope.launch {
            sessionQueue.clear()
          }
        }

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
