// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownloadKey
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.UserId
import kotlinx.serialization.Serializable

/**
 * Everything needed to (re)start an offline download without the library item at hand: written
 * once when the download is requested, so an interrupted download resumes after a restart even
 * when the server is unreachable or the item's cache is gone.
 *
 * Completion is not recorded here — a file is complete exactly when its final (non-`.part`) file
 * exists, which an atomic rename guarantees.
 */
@Serializable
internal data class DownloadManifest(
  val libraryItemId: LibraryItemId,
  val episodeId: PodcastEpisodeId? = null,
  /** The account whose credentials fetch the files. */
  val userId: UserId,
  val serverUrl: String,
  val title: String = "",
  val subtitle: String = "",
  val createdAtMs: Long,
  val files: List<File>,
) {

  val key: OfflineDownloadKey get() = OfflineDownloadKey(libraryItemId, episodeId)

  @Serializable
  data class File(
    /** The audio track's index on the item, matching [app.campfire.core.model.AudioTrack.index]. */
    val trackIndex: Int,
    val url: String,
    /** The server-reported file size, or <= 0 when unknown. */
    val expectedBytes: Long,
    /** The on-disk file name inside the download's directory. */
    val fileName: String,
  )
}
