// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(PlaylistsScreen::class, UserScope::class)
@Inject
class PlaylistsPresenter(
  @Assisted private val navigator: Navigator,
  private val playlistsRepository: PlaylistsRepository,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<PlaylistsUiState> {

  @Composable
  override fun present(): PlaylistsUiState {
    val playlistContentState by remember {
      playlistsRepository.observeAllPlaylists()
        .map { LoadState.Loaded(it) }
        .catch<LoadState<out List<Playlist>>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    val displayState by remember {
      settings.observePlaylistsDisplayState()
    }.collectAsState()

    return PlaylistsUiState(
      playlistContentState = playlistContentState,
      displayState = displayState,
    ) { event ->
      when (event) {
        PlaylistsUiEvent.Back -> navigator.pop()

        PlaylistsUiEvent.ToggleDisplayState -> {
          analytics.send(ActionEvent("playlists_display_state", "toggle"))
          settings.playlistsDisplayState = displayState.next()
        }

        is PlaylistsUiEvent.PlaylistClick -> {
          analytics.send(ContentSelected(ContentType.Collection))
          navigator.goTo(PlaylistDetailScreen(event.playlist))
        }
      }
    }
  }
}
