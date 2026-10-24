// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.api

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId
import kotlinx.coroutines.flow.Flow

/**
 * Per-item / per-episode progress repository. Methods accept an optional [episodeId] so
 * the same surface can read/write book progress (single row per library item) and podcast
 * episode progress (one row per episode). Pass `null`/omit to target the book or item-level
 * row; pass an episode id to target that episode's progress.
 */
interface MediaProgressRepository {

  fun observeProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
    refresh: Boolean = false,
  ): Flow<MediaProgress?>

  suspend fun getProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
    fresh: Boolean = false,
  ): MediaProgress?

  fun observeAllProgress(): Flow<List<MediaProgress>>

  /**
   * Persist [newProgress] to local storage.
   *
   * @param force Bypasses the synchronizer's stale-write throttle.
   * @param skipUpload When `true`, the local write happens but the synchronizer's upload back
   *   to the server is suppressed. Use this when the progress originated remotely (e.g. came
   *   in over the socket from another client) so we don't echo it back to the server.
   * @param onlyIfFresher When `true`, the write is skipped entirely if the stored row has a
   *   newer [MediaProgress.lastUpdate] than [newProgress]. Use this for remotely-originated
   *   writes so a device's own (throttled, therefore stale) server echo can never clobber
   *   the fresher local row it is continuously writing during playback.
   */
  suspend fun updateProgress(
    newProgress: MediaProgress,
    force: Boolean = false,
    skipUpload: Boolean = false,
    onlyIfFresher: Boolean = false,
  )

  /**
   * Save a reading position in [libraryItemId]'s ebook, here first and then to the server with
   * only the ebook fields, so the item's listening progress is left alone. A position that can't
   * reach the server yet is sent once it can.
   *
   * @param location the position in the format the official Audiobookshelf readers use: an
   * epub.js CFI for EPUB, a 1-based page number for comics
   * @param progress how far through the book, 0..1
   */
  suspend fun updateEbookProgress(
    libraryItemId: LibraryItemId,
    location: String,
    progress: Float,
  )

  suspend fun deleteProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  suspend fun markFinished(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  suspend fun markNotFinished(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  /**
   * Change when the finished progress of [libraryItemId], or of its [episodeId], was finished.
   * The local row takes the new date first, so a sync from playback carries it rather than the
   * old one, and goes back to the old date if the server doesn't take it.
   *
   * @param finishedAt the new finish time, in epoch milliseconds
   * @return a failure when there is no finished progress to change or the server refused it
   */
  suspend fun setFinishedAt(
    libraryItemId: LibraryItemId,
    finishedAt: Long,
    episodeId: PodcastEpisodeId? = null,
  ): Result<Unit>

  /**
   * Change when the progress of [libraryItemId], or of its [episodeId], was started.
   *
   * Audiobookshelf only sets a progress's start when it creates the progress, so this deletes the
   * server's copy and creates it again with everything else it held, then checks the start took.
   * The progress gets a new id, and the server counts it as just updated. Don't call this while
   * the item is playing here, or a sync from playback can create the progress in between.
   *
   * @param startedAt the new start time, in epoch milliseconds
   * @return a failure, a [StartedAtNotChanged] where the server was reached, saying how far it got
   */
  suspend fun setStartedAt(
    libraryItemId: LibraryItemId,
    startedAt: Long,
    episodeId: PodcastEpisodeId? = null,
  ): Result<Unit>

  /**
   * Put this device's copy of the progress of [libraryItemId], or of its [episodeId], back on the
   * server, for when [setStartedAt] couldn't.
   */
  suspend fun restoreProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  ): Result<Unit>

  /**
   * Mark every item in [keys] finished with a single request to the server.
   */
  suspend fun markAllFinished(keys: List<MediaProgressKey>)

  /**
   * Mark every item in [keys] not finished, removing its progress the same way
   * [markNotFinished] does for a single item.
   */
  suspend fun markAllNotFinished(keys: List<MediaProgressKey>)
}
