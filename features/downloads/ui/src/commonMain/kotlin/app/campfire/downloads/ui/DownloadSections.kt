// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.runtime.Immutable
import app.campfire.core.model.LibraryItem
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent

/** The kinds of thing that can be downloaded, in the order Downloads lists them. */
enum class DownloadKind {
  Audiobook,
  Podcast,
  Ebook,
}

/** The kind of thing downloaded, which is known even when its item can't be found. */
val DownloadEntry.kind: DownloadKind
  get() = when (download.content) {
    is DownloadContent.Audiobook -> DownloadKind.Audiobook
    is DownloadContent.Episode -> DownloadKind.Podcast
    is DownloadContent.Ebook -> DownloadKind.Ebook
  }

/** Whether the download can be played or read: it's whole, and its item is known. */
private val DownloadEntry.isFinished: Boolean
  get() = this !is DownloadEntry.Unavailable && download.state == Download.State.Downloaded

/** A podcast's downloaded episodes, most recently downloaded first. */
@Immutable
data class ShowDownloads(
  val libraryItem: LibraryItem,
  val episodes: List<DownloadEntry.Episode>,
) {
  val sizeInBytes: Long get() = episodes.sumOf { it.sizeInBytes }
}

/**
 * The downloads as Downloads lists them: those that still need something first, then what's ready
 * by kind, then what's kept for items that can't be found. Each keeps the order it was given in.
 */
@Immutable
data class DownloadSections(
  /** Queued, downloading, stopped and failed downloads, whatever their kind. */
  val inProgress: List<DownloadEntry> = emptyList(),
  val audiobooks: List<DownloadEntry.Book> = emptyList(),
  /** Finished episodes under their podcasts, in the order of each podcast's first episode. */
  val shows: List<ShowDownloads> = emptyList(),
  val ebooks: List<DownloadEntry.Ebook> = emptyList(),
  val unavailable: List<DownloadEntry.Unavailable> = emptyList(),
)

/** These downloads in their sections, or only those of [kind] when it's given. */
internal fun List<DownloadEntry>.asSections(kind: DownloadKind? = null): DownloadSections {
  val entries = if (kind == null) this else filter { it.kind == kind }
  val finished = entries.filter { it.isFinished }
  return DownloadSections(
    inProgress = entries.filter { !it.isFinished && it !is DownloadEntry.Unavailable },
    audiobooks = finished.filterIsInstance<DownloadEntry.Book>(),
    shows = finished.filterIsInstance<DownloadEntry.Episode>()
      .groupBy { it.libraryItem.id }
      .map { (_, episodes) -> ShowDownloads(episodes.first().libraryItem, episodes) },
    ebooks = finished.filterIsInstance<DownloadEntry.Ebook>(),
    unavailable = entries.filterIsInstance<DownloadEntry.Unavailable>(),
  )
}

/** How many downloads of one kind there are and what they take up. */
@Immutable
data class KindSummary(
  val kind: DownloadKind,
  val count: Int,
  val sizeInBytes: Long,
)

/** How many downloads there are and what they take up, in all and for each kind that has any. */
@Immutable
data class DownloadsSummary(
  val count: Int,
  val sizeInBytes: Long,
  val kinds: List<KindSummary>,
)

/**
 * What the download takes up on this device now: all of a finished one, and what's been fetched
 * so far of one that isn't.
 */
internal val DownloadEntry.sizeOnDevice: Long
  get() = if (isFinished || this is DownloadEntry.Unavailable) sizeInBytes else download.bytes.coerceAtLeast(0L)

internal fun List<DownloadEntry>.summary(): DownloadsSummary = DownloadsSummary(
  count = size,
  sizeInBytes = sumOf { it.sizeOnDevice },
  kinds = DownloadKind.entries.mapNotNull { kind ->
    val ofKind = filter { it.kind == kind }
    if (ofKind.isEmpty()) null else KindSummary(kind, ofKind.size, ofKind.sumOf { it.sizeOnDevice })
  },
)
