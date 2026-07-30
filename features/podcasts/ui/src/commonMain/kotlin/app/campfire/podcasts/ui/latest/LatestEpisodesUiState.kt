// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.latest

import androidx.paging.compose.LazyPagingItems
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Session
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.user.api.MediaProgressKey
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LatestEpisodesUiState(
  val currentSession: Session?,
  val episodes: LazyPagingItems<LatestEpisode>,
  val mediaProgress: Map<MediaProgressKey, MediaProgress>,
  val addToPlaylistDialog: AddToPlaylistDialog,
  val eventSink: (LatestEpisodesUiEvent) -> Unit,
) : CircuitUiState

sealed interface LatestEpisodesUiEvent : CircuitUiEvent {
  data class PlayEpisode(
    val libraryItemId: LibraryItemId,
    val episodeId: PodcastEpisodeId,
  ) : LatestEpisodesUiEvent

  data class OpenPodcast(
    val episode: LatestEpisode,
  ) : LatestEpisodesUiEvent

  data class MarkFinished(
    val episode: LatestEpisode,
  ) : LatestEpisodesUiEvent

  data class MarkNotFinished(
    val episode: LatestEpisode,
  ) : LatestEpisodesUiEvent
}
