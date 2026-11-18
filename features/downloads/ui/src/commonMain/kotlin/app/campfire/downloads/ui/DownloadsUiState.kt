// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisode
import app.campfire.offline.Download
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class DownloadsUiState(
  val downloads: LoadState<out List<DownloadEntry>>,
  val eventSink: (DownloadsUiEvent) -> Unit,
) : CircuitUiState

@Immutable
sealed interface DownloadEntry {
  /** The item the download belongs to, or null when it can't be found. */
  val libraryItem: LibraryItem?
  val download: Download

  data class Book(
    override val libraryItem: LibraryItem,
    override val download: Download,
  ) : DownloadEntry

  data class Episode(
    override val libraryItem: LibraryItem,
    val episode: PodcastEpisode,
    override val download: Download,
  ) : DownloadEntry

  /** An ebook kept by the reader to read offline. */
  data class Ebook(
    override val libraryItem: LibraryItem,
    override val download: Download,
  ) : DownloadEntry

  /**
   * Everything kept of an item that can't be found, because it's gone from the server or was never
   * cached and the server can't be reached. One row stands for all of the item's downloads, as
   * they can only be removed together: [download] is the latest of them and [sizeInBytes] what
   * they take up between them.
   */
  data class Unavailable(
    override val download: Download,
    val sizeInBytes: Long,
  ) : DownloadEntry {
    override val libraryItem: LibraryItem? get() = null
  }
}

sealed interface DownloadsUiEvent : CircuitUiEvent {
  data object Back : DownloadsUiEvent
  data class DownloadClick(val entry: DownloadEntry) : DownloadsUiEvent

  /** Starts a failed or stopped download again. */
  data class RetryDownload(val entry: DownloadEntry) : DownloadsUiEvent

  /** Removes a download from this device, cancelling it if it's still going. */
  data class DeleteDownload(val entry: DownloadEntry) : DownloadsUiEvent
}
