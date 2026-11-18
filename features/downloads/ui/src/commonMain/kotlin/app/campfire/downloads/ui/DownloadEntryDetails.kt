// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import kotlin.math.roundToInt

/**
 * What the download takes up once it's whole: an ebook's own file, as an item may hold audio as
 * well, and otherwise the size of the book or the episode. Zero when it isn't known.
 */
internal val DownloadEntry.sizeInBytes: Long
  get() = when (this) {
    is DownloadEntry.Book -> libraryItem.media.sizeInBytes
    is DownloadEntry.Episode -> episode.sizeInBytes
    is DownloadEntry.Ebook -> download.totalBytes.takeIf { it > 0L } ?: libraryItem.media.sizeInBytes
    is DownloadEntry.Unavailable -> sizeInBytes
  }

/**
 * The file format to name the download by, such as M4B or EPUB, which also tells a book's audio
 * from its ebook when both are downloaded. Null for an episode, and for a book mixing formats,
 * where no single one reads cleanly.
 */
internal val DownloadEntry.formatLabel: String?
  get() = when (this) {
    is DownloadEntry.Book ->
      libraryItem.media.tracks
        .map { it.metadata.ext.removePrefix(".").uppercase() }
        .filter { it.isNotBlank() }
        .distinct()
        .singleOrNull()

    is DownloadEntry.Ebook -> libraryItem.media.ebookFormat?.uppercase()?.takeIf { it.isNotBlank() }

    is DownloadEntry.Episode,
    is DownloadEntry.Unavailable,
    -> null
  }

/** How far along the download is, from 0 to 100, or null when it can't be told. */
internal val DownloadEntry.percent: Int?
  get() = download.progress?.let { (it.coerceIn(0f, 1f) * 100).roundToInt() }
