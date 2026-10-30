// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisode
import app.campfire.offline.Download
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class DownloadsUiState(
  val downloads: List<DownloadEntry>,
  val eventSink: (DownloadsUiEvent) -> Unit,
) : CircuitUiState

@Immutable
sealed interface DownloadEntry {
  val libraryItem: LibraryItem
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
}

sealed interface DownloadsUiEvent : CircuitUiEvent {
  data object Back : DownloadsUiEvent
  data class DownloadClick(val entry: DownloadEntry) : DownloadsUiEvent
  data class DeleteDownload(val entry: DownloadEntry) : DownloadsUiEvent
}
