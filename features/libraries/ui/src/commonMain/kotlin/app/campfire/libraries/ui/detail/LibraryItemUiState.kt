// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.campfire.bookinfo.api.ProviderId
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PlaylistId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.SeriesSequence
import app.campfire.core.model.Session
import app.campfire.core.model.User
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import com.r0adkll.swatchbuckler.compose.Swatch
import com.r0adkll.swatchbuckler.compose.Theme
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class LibraryItemUiState(
  val user: User,
  val libraryItem: LibraryItem?,
  val swatch: Swatch? = null,
  val theme: Theme? = null,
  val contentState: LoadState<out ContentUiState>,
  val errorMessage: String? = null,
  val eventSink: (LibraryItemUiEvent) -> Unit,
) : CircuitUiState

@Immutable
data class ContentUiState(
  val slots: List<ContentSlot>,
  val eventSink: (LibraryItemUiEvent) -> Unit,
) : CircuitUiState

sealed interface SessionUiState {
  data object None : SessionUiState
  data class Current(val session: Session) : SessionUiState

  fun sessionOrNull(): Session? = (this as? Current)?.session
}

sealed interface LibraryItemUiEvent : CircuitUiEvent {
  data object AddToQueue : LibraryItemUiEvent
  data object RemoveFromQueue : LibraryItemUiEvent
  data class SeedColorChange(val seedColor: Color) : LibraryItemUiEvent

  data class PlayClick(val method: PlayMethod?) : LibraryItemUiEvent
  data class PlayEpisodeClick(val episode: PodcastEpisode) : LibraryItemUiEvent
  data class SeriesClick(val item: LibraryItem, val series: SeriesSequence) : LibraryItemUiEvent
  data class DiscardProgress(val item: LibraryItem) : LibraryItemUiEvent
  data class MarkFinished(val item: LibraryItem) : LibraryItemUiEvent
  data class MarkNotFinished(val item: LibraryItem) : LibraryItemUiEvent
  data class MarkEpisodeFinished(val episode: PodcastEpisode) : LibraryItemUiEvent
  data class MarkEpisodeNotFinished(val episode: PodcastEpisode) : LibraryItemUiEvent
  data class AuthorClick(val item: LibraryItem, val author: String) : LibraryItemUiEvent
  data class NarratorClick(val item: LibraryItem, val narrator: String) : LibraryItemUiEvent
  data class ChapterClick(val item: LibraryItem, val chapter: Chapter) : LibraryItemUiEvent
  data object ExpandChaptersClick : LibraryItemUiEvent
  data class AudioTrackClick(val item: LibraryItem, val track: AudioTrack) : LibraryItemUiEvent
  data class TimeInBookChange(val enabled: Boolean) : LibraryItemUiEvent

  data class DownloadClick(val doNotShowAgain: Boolean = true) : LibraryItemUiEvent
  data object RemoveDownloadClick : LibraryItemUiEvent
  data object StopDownloadClick : LibraryItemUiEvent

  data class DownloadEpisodeClick(
    val episode: PodcastEpisode,
    val doNotShowAgain: Boolean = true,
  ) : LibraryItemUiEvent
  data class RemoveEpisodeDownloadClick(val episode: PodcastEpisode) : LibraryItemUiEvent
  data class StopEpisodeDownloadClick(val episode: PodcastEpisode) : LibraryItemUiEvent

  data class OpenPlaylist(val playlistId: PlaylistId, val isCreated: Boolean) : LibraryItemUiEvent
  data class OpenEpisode(val episode: PodcastEpisode) : LibraryItemUiEvent
  data object FindEpisodes : LibraryItemUiEvent
  data object OpenDownloads : LibraryItemUiEvent

  data class OpenProviderPage(val url: String) : LibraryItemUiEvent
  data object RelinkProvider : LibraryItemUiEvent
  data class SelectCommunitySource(val providerId: ProviderId) : LibraryItemUiEvent

  data class DeleteItemClick(val hardDelete: Boolean) : LibraryItemUiEvent
  data object ClearError : LibraryItemUiEvent

  data object OnBack : LibraryItemUiEvent
}
