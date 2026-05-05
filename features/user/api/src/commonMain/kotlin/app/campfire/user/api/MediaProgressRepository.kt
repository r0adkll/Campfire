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

  suspend fun updateProgress(
    newProgress: MediaProgress,
    force: Boolean = false,
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
}
