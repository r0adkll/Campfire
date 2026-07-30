// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.podcast.episode

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.SessionUiState
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import com.slack.circuit.runtime.CircuitUiState
import kotlin.time.Duration

data class PodcastEpisodeUiState(
  val libraryItem: LibraryItem,
  val episode: PodcastEpisode,
  val progress: MediaProgress?,
  val playbackSpeed: Float,
  val isPlaying: Boolean,
  val isQueued: Boolean,
  val hasSession: Boolean,
  val sessionState: SessionUiState,
  val offlineDownload: OfflineDownload?,
  val showConfirmDownloadDialog: Boolean,
  val addToPlaylistDialog: AddToPlaylistDialog,
  val eventSink: (PodcastEpisodeUiEvent) -> Unit,
) : CircuitUiState

sealed interface PodcastEpisodeUiEvent {
  data object PlayClick : PodcastEpisodeUiEvent
  data object MarkFinished : PodcastEpisodeUiEvent
  data object MarkNotFinished : PodcastEpisodeUiEvent
  data object DiscardProgress : PodcastEpisodeUiEvent
  data class Seek(val position: Duration) : PodcastEpisodeUiEvent
  data object AddToQueue : PodcastEpisodeUiEvent
  data object RemoveFromQueue : PodcastEpisodeUiEvent
  data class DownloadClick(val doNotShowAgain: Boolean = true) : PodcastEpisodeUiEvent
  data object RemoveDownloadClick : PodcastEpisodeUiEvent
  data object StopDownloadClick : PodcastEpisodeUiEvent
}
