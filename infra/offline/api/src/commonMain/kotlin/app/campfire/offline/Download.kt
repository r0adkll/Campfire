// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.offline.OfflineStatus

/** Something of a library item that can be kept on this device. */
sealed interface DownloadContent {
  val itemId: LibraryItemId

  /** Every audio track of a book. */
  data class Audiobook(override val itemId: LibraryItemId) : DownloadContent

  /** One podcast episode's audio. */
  data class Episode(
    override val itemId: LibraryItemId,
    val episodeId: PodcastEpisodeId,
  ) : DownloadContent

  /** An item's ebook file. */
  data class Ebook(override val itemId: LibraryItemId) : DownloadContent
}

/**
 * [content] kept on this device, or on its way. Content that isn't downloaded at all has no
 * [Download].
 */
data class Download(
  val content: DownloadContent,
  val state: State,
  /** Bytes on this device so far. */
  val bytes: Long = 0L,
  /** The whole download's size, or -1 until it's known. */
  val totalBytes: Long = -1L,
  /** How far along, 0..1, or null when it can't be told. */
  val progress: Float? = null,
  /** When it last changed, in epoch milliseconds, or -1 if not known. */
  val updatedAtMs: Long = -1L,
) {

  enum class State {
    /** Waiting to start, including for the network it's allowed to use. */
    Queued,
    Downloading,

    /** Stopped part way by the user. */
    Stopped,
    Downloaded,
    Failed,
  }

  /** Whether [content] can be used offline. Only a finished download can. */
  val isAvailable: Boolean
    get() = state == State.Downloaded

  /** Whether it's still to finish, rather than downloaded, stopped or failed. */
  val isInProgress: Boolean
    get() = state == State.Queued || state == State.Downloading
}

/** Everything of one item kept on this device. */
data class ItemDownloads(
  val itemId: LibraryItemId,
  val audiobook: Download? = null,
  val ebook: Download? = null,
  val episodes: Map<PodcastEpisodeId, Download> = emptyMap(),
) {

  val all: List<Download>
    get() = listOfNotNull(audiobook, ebook) + episodes.values

  /**
   * The download that best describes the item as a whole: anything downloaded marks it
   * downloaded, otherwise anything downloading or queued shows that.
   */
  val furthest: Download?
    get() = all.maxByOrNull { it.state.rank }
}

/** [downloads] grouped by the item they belong to. */
fun List<Download>.byItem(): Map<LibraryItemId, ItemDownloads> = groupBy { it.content.itemId }
  .mapValues { (itemId, downloads) ->
    ItemDownloads(
      itemId = itemId,
      audiobook = downloads.firstOrNull { it.content is DownloadContent.Audiobook },
      ebook = downloads.firstOrNull { it.content is DownloadContent.Ebook },
      episodes = downloads.mapNotNull { download ->
        (download.content as? DownloadContent.Episode)?.let { it.episodeId to download }
      }.toMap(),
    )
  }

/**
 * The status to mark an item with in lists, or one of its podcast episodes when [episodeId] is
 * given.
 */
fun Map<LibraryItemId, ItemDownloads>.statusOf(
  itemId: LibraryItemId,
  episodeId: PodcastEpisodeId? = null,
): OfflineStatus {
  val downloads = get(itemId) ?: return OfflineStatus.None
  val download = if (episodeId != null) downloads.episodes[episodeId] else downloads.furthest
  return download.asOfflineStatus()
}

/** How a download shows in the UI. */
fun Download?.asOfflineStatus(): OfflineStatus = when (this?.state) {
  null -> OfflineStatus.None
  Download.State.Queued -> OfflineStatus.Queued
  Download.State.Downloading -> OfflineStatus.Downloading(progress ?: 0f)
  Download.State.Downloaded -> OfflineStatus.Available
  Download.State.Stopped,
  Download.State.Failed,
  -> OfflineStatus.Failed
}

private val Download.State.rank: Int
  get() = when (this) {
    Download.State.Stopped -> 0
    Download.State.Failed -> 1
    Download.State.Queued -> 2
    Download.State.Downloading -> 3
    Download.State.Downloaded -> 4
  }
