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
  /** The downloads to list, without [pendingDelete] or any already deleted from here. */
  val downloads: LoadState<out List<DownloadEntry>>,
  /**
   * The download just taken off the list, which can still be put back: it's only removed from
   * this device on [DownloadsUiEvent.CommitDelete], when another is deleted, or on leaving.
   */
  val pendingDelete: DownloadEntry?,
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

/** Tells this entry's row from every other, and stays the same as its download goes. */
val DownloadEntry.key: String
  get() = when (this) {
    is DownloadEntry.Book -> libraryItem.id
    is DownloadEntry.Episode -> "${libraryItem.id}:${episode.id}"
    is DownloadEntry.Ebook -> "${libraryItem.id}:ebook"
    is DownloadEntry.Unavailable -> "${download.content.itemId}:unavailable"
  }

sealed interface DownloadsUiEvent : CircuitUiEvent {
  data object Back : DownloadsUiEvent
  data class DownloadClick(val entry: DownloadEntry) : DownloadsUiEvent

  /** Plays a downloaded book or episode, or opens a downloaded ebook to read. */
  data class PlayClick(val entry: DownloadEntry) : DownloadsUiEvent

  /** Starts a failed or stopped download again. */
  data class RetryDownload(val entry: DownloadEntry) : DownloadsUiEvent

  /**
   * Takes a download off the list, cancelling it if it's still going. It becomes the
   * [DownloadsUiState.pendingDelete], and the one before it, if any, is removed for good.
   */
  data class DeleteDownload(val entry: DownloadEntry) : DownloadsUiEvent

  /** Puts the [DownloadsUiState.pendingDelete] back on the list. */
  data class UndoDelete(val entry: DownloadEntry) : DownloadsUiEvent

  /** Removes the [DownloadsUiState.pendingDelete] from this device, now it can't be undone. */
  data class CommitDelete(val entry: DownloadEntry) : DownloadsUiEvent
}
