// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.audioplayer.PlaybackController
import app.campfire.common.screens.QueueScreen
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.sessions.api.QueuedEntry
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import app.campfire.sessions.ui.playback.QueueUiEvent
import app.campfire.sessions.ui.playback.presentQueueState
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@CircuitInject(QueueScreen::class, UserScope::class)
@Inject
class QueuePresenter(
  private val navigator: Navigator,
  private val sessionQueue: SessionQueue,
  private val sessionsRepository: SessionsRepository,
  private val playbackController: PlaybackController,
) : Presenter<QueueScreenUiState> {

  @Composable
  override fun present(): QueueScreenUiState {
    // Only what's playing, not where it's at: the session updates every few hundred
    // milliseconds while playing, which would otherwise recompose the whole list.
    val nowPlaying by remember {
      sessionsRepository.observeCurrentSession()
        .map { session -> session?.let { QueuedEntry(it.libraryItem, it.episode) } }
        .distinctUntilChanged()
    }.collectAsState(null)

    val queueState = presentQueueState(sessionQueue, playbackController)

    return QueueScreenUiState(
      nowPlaying = nowPlaying,
      queueState = queueState,
    ) { event ->
      when (event) {
        QueueScreenUiEvent.Back -> navigator.pop()

        QueueScreenUiEvent.PlayQueue -> queueState.queue.firstOrNull()?.let { first ->
          queueState.eventSink(QueueUiEvent.QueueItemClick(first))
        }

        is QueueScreenUiEvent.OpenItem -> navigator.goTo(
          LibraryItemScreen(
            libraryItemId = event.entry.libraryItemId,
            episodeId = event.entry.episodeId,
          ),
        )
      }
    }
  }
}
