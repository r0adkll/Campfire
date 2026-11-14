// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.latest

import androidx.paging.compose.LazyPagingItems
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.sessions.api.PlayingItem
import app.campfire.user.api.MediaProgressKey
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LatestEpisodesUiState(
  val playingItem: PlayingItem?,
  val episodes: LazyPagingItems<LatestEpisode>,
  val mediaProgress: Map<MediaProgressKey, MediaProgress>,
  /** The ids of the episodes in the playback queue. */
  val queuedEpisodeIds: Set<PodcastEpisodeId>,
  val addToPlaylistDialog: AddToPlaylistDialog,
  /** Episodes the server is downloading, shown as a banner while there are any. */
  val serverDownloadCount: Int = 0,
  /** Whether the account may delete episodes from the server, which offers it on each row. */
  val canDeleteEpisodes: Boolean = false,
  /** Whether deleting an episode starts with deleting its file chosen, the last choice confirmed. */
  val deleteFileByDefault: Boolean = true,
  val eventSink: (LatestEpisodesUiEvent) -> Unit,
) : CircuitUiState

sealed interface LatestEpisodesUiEvent : CircuitUiEvent {
  data object ServerDownloadsClick : LatestEpisodesUiEvent

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

  data class AddToQueue(
    val episode: LatestEpisode,
  ) : LatestEpisodesUiEvent

  data class RemoveFromQueue(
    val episode: LatestEpisode,
  ) : LatestEpisodesUiEvent

  /** Delete the episode from the server, and its audio file too if [hardDelete]. */
  data class DeleteEpisode(
    val episode: LatestEpisode,
    val hardDelete: Boolean,
  ) : LatestEpisodesUiEvent
}
