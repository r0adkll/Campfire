package app.campfire.user.test

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.user.api.MediaProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMediaProgressRepository : MediaProgressRepository {

  val invocations = mutableListOf<Invocation>()

  val progressFlow = MutableStateFlow<MediaProgress?>(null)
  override fun observeProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
    refresh: Boolean,
  ): Flow<MediaProgress?> {
    invocations += Invocation.ObserveProgress(libraryItemId, episodeId)
    return progressFlow
  }

  var progress: MediaProgress? = null
  override suspend fun getProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
    fresh: Boolean,
  ): MediaProgress? {
    invocations += Invocation.GetProgress(libraryItemId, episodeId)
    return progress
  }

  val allProgressFlow = MutableStateFlow<List<MediaProgress>>(emptyList())
  override fun observeAllProgress(): Flow<List<MediaProgress>> {
    invocations += Invocation.ObserveAllProgress
    return allProgressFlow
  }

  override suspend fun updateProgress(
    newProgress: MediaProgress,
    force: Boolean,
    skipUpload: Boolean,
  ) {
    invocations += Invocation.UpdateProgress(newProgress, force, skipUpload)
  }

  override suspend fun deleteProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    invocations += Invocation.DeleteProgress(libraryItemId, episodeId)
  }

  override suspend fun markFinished(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    invocations += Invocation.MarkFinished(libraryItemId, episodeId)
  }

  override suspend fun markNotFinished(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    invocations += Invocation.MarkNotFinished(libraryItemId, episodeId)
  }

  sealed interface Invocation {
    data class GetProgress(
      val libraryItemId: LibraryItemId,
      val episodeId: PodcastEpisodeId? = null,
    ) : Invocation
    data class ObserveProgress(
      val libraryItemId: LibraryItemId,
      val episodeId: PodcastEpisodeId? = null,
    ) : Invocation
    data object ObserveAllProgress : Invocation
    data class UpdateProgress(
      val newProgress: MediaProgress,
      val force: Boolean,
      val skipUpload: Boolean,
    ) : Invocation
    data class DeleteProgress(
      val libraryItemId: LibraryItemId,
      val episodeId: PodcastEpisodeId? = null,
    ) : Invocation
    data class MarkFinished(
      val libraryItemId: LibraryItemId,
      val episodeId: PodcastEpisodeId? = null,
    ) : Invocation
    data class MarkNotFinished(
      val libraryItemId: LibraryItemId,
      val episodeId: PodcastEpisodeId? = null,
    ) : Invocation
  }
}
