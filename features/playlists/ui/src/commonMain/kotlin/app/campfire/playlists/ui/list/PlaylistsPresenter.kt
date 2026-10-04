// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.Playlist
import app.campfire.playlists.api.PlaylistsRepository
import app.campfire.playlists.api.screen.PlaylistDetailScreen
import app.campfire.playlists.api.screen.PlaylistsScreen
import app.campfire.settings.api.LibraryViewSettings
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@CircuitInject(PlaylistsScreen::class, UserScope::class)
@Inject
class PlaylistsPresenter(
  private val navigator: Navigator,
  private val playlistsRepository: PlaylistsRepository,
  private val libraryViewSettings: LibraryViewSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<PlaylistsUiState> {

  @Composable
  override fun present(): PlaylistsUiState {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val playlistContentState by remember {
      playlistsRepository.observeAllPlaylists()
        .map { LoadState.Loaded(it) }
        .catch<LoadState<out List<Playlist>>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    val displayState by remember {
      libraryViewSettings.observePlaylistsDisplayState()
    }.collectAsState(null)

    return PlaylistsUiState(
      playlistContentState = playlistContentState,
      displayState = displayState,
      isRefreshing = isRefreshing,
    ) { event ->
      when (event) {
        PlaylistsUiEvent.Back -> navigator.pop()

        PlaylistsUiEvent.Refresh -> if (!isRefreshing) {
          isRefreshing = true
          scope.launch {
            try {
              playlistsRepository.refreshPlaylists()
            } finally {
              isRefreshing = false
            }
          }
        }

        PlaylistsUiEvent.ToggleDisplayState -> {
          analytics.send(ActionEvent("playlists_display_state", "toggle"))
          displayState?.next()?.let(libraryViewSettings::setPlaylistsDisplayState)
        }

        is PlaylistsUiEvent.PlaylistClick -> {
          analytics.send(ContentSelected(ContentType.Collection))
          navigator.goTo(PlaylistDetailScreen(event.playlist))
        }
      }
    }
  }
}
