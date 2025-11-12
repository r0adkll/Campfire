package app.campfire.user.test

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.user.api.MediaProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMediaProgressRepository : MediaProgressRepository {

  val invocations = mutableListOf<Invocation>()

  val progressFlow = MutableStateFlow<MediaProgress?>(null)
  override fun observeProgress(libraryItemId: LibraryItemId): Flow<MediaProgress?> {
    invocations += Invocation.ObserveProgress(libraryItemId)
    return progressFlow
  }

  var progress: MediaProgress? = null
  override suspend fun getProgress(libraryItemId: LibraryItemId): MediaProgress? {
    invocations += Invocation.GetProgress(libraryItemId)
    return progress
  }

  val allProgressFlow = MutableStateFlow<List<MediaProgress>>(emptyList())
  override fun observeAllProgress(): Flow<List<MediaProgress>> {
    invocations += Invocation.ObserveAllProgress
    return allProgressFlow
  }

  override suspend fun updateProgress(newProgress: MediaProgress, force: Boolean) {
    invocations += Invocation.UpdateProgress(newProgress, force)
  }

  override suspend fun deleteProgress(libraryItemId: LibraryItemId) {
    invocations += Invocation.DeleteProgress(libraryItemId)
  }

  override suspend fun markFinished(libraryItemId: LibraryItemId) {
    invocations += Invocation.MarkFinished(libraryItemId)
  }

  override suspend fun markNotFinished(libraryItemId: LibraryItemId) {
    invocations += Invocation.MarkNotFinished(libraryItemId)
  }

  sealed interface Invocation {
    data class GetProgress(val libraryItemId: LibraryItemId) : Invocation
    data class ObserveProgress(val libraryItemId: LibraryItemId) : Invocation
    data object ObserveAllProgress : Invocation
    data class UpdateProgress(val newProgress: MediaProgress, val force: Boolean) : Invocation
    data class DeleteProgress(val libraryItemId: LibraryItemId) : Invocation
    data class MarkFinished(val libraryItemId: LibraryItemId) : Invocation
    data class MarkNotFinished(val libraryItemId: LibraryItemId) : Invocation
  }
}
