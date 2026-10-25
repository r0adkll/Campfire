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
import app.campfire.libraries.ui.detail.dates.ListeningDate
import app.campfire.libraries.ui.detail.manage.QuickMatchChoice
import com.r0adkll.swatchbuckler.compose.Swatch
import com.r0adkll.swatchbuckler.compose.Theme
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.datetime.LocalDate

@Immutable
data class LibraryItemUiState(
  val user: User,
  val libraryItem: LibraryItem?,
  val swatch: Swatch? = null,
  val theme: Theme? = null,
  val contentState: LoadState<out ContentUiState>,
  /** Something to tell the user in a snackbar, cleared by [LibraryItemUiEvent.ClearMessage]. */
  val message: String? = null,
  /** The source a quick match would ask, by name, while asking whether to go ahead */
  val quickMatchSource: String? = null,
  val eventSink: (LibraryItemUiEvent) -> Unit,
) : CircuitUiState

@Immutable
data class ContentUiState(
  val slots: List<ContentSlot>,
  /** Something to tell the user, such as how a change went, cleared by [LibraryItemUiEvent.ClearMessage]. */
  val message: String? = null,
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

  /** Read the item's ebook, for an item with audio too. */
  data object ReadClick : LibraryItemUiEvent

  /** Read the item's ebook from [href], an entry in its contents. */
  data class ReadAt(val href: String) : LibraryItemUiEvent
  data class PlayEpisodeClick(val episode: PodcastEpisode) : LibraryItemUiEvent
  data class SeriesClick(val item: LibraryItem, val series: SeriesSequence) : LibraryItemUiEvent
  data class DiscardProgress(val item: LibraryItem) : LibraryItemUiEvent
  data class MarkFinished(val item: LibraryItem) : LibraryItemUiEvent
  data class MarkNotFinished(val item: LibraryItem) : LibraryItemUiEvent

  /** Pick a new day for one of the item's listening dates. */
  data class EditListeningDate(val date: ListeningDate) : LibraryItemUiEvent

  /** Put the listening date being picked away unchanged. */
  data object DismissListeningDate : LibraryItemUiEvent

  /**
   * Save [day] as the new day of the item's listening [date]. A new start waits on
   * [ConfirmStartedDate], as changing it recreates the progress.
   */
  data class ChangeListeningDate(val date: ListeningDate, val day: LocalDate) : LibraryItemUiEvent

  /** Go ahead with recreating the progress to change when it started. */
  data object ConfirmStartedDate : LibraryItemUiEvent

  /** Try again at putting back the progress a changed start left off the server. */
  data object RestoreProgress : LibraryItemUiEvent
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

  /** Have the server scan the item's files again. */
  data object RescanClick : LibraryItemUiEvent

  /** Open the editor for the item's details. */
  data object EditClick : LibraryItemUiEvent

  /** Ask whether to quick match the item against its library's source. */
  data object QuickMatchClick : LibraryItemUiEvent

  /** Quick match the item, replacing what [choice] lets it. */
  data class QuickMatch(val choice: QuickMatchChoice) : LibraryItemUiEvent

  /** Back out of quick matching the item. */
  data object DismissQuickMatch : LibraryItemUiEvent
  data object ClearMessage : LibraryItemUiEvent

  data object OnBack : LibraryItemUiEvent
}
