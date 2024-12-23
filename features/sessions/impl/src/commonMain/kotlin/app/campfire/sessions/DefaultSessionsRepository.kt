package app.campfire.sessions

import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.Session
import app.campfire.core.time.FatherTime
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.network.AudioBookShelfApi
import app.campfire.sessions.api.SessionsRepository
import app.campfire.sessions.db.MediaProgressDataSource
import app.campfire.sessions.db.SessionDataSource
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultSessionsRepository(
  private val fatherTime: FatherTime,
  private val libraryItemRepository: LibraryItemRepository,
  private val dataSource: SessionDataSource,
  private val mediaProgressDataSource: MediaProgressDataSource,
  private val api: AudioBookShelfApi,
) : SessionsRepository {

  override fun observeCurrentSession(): Flow<Session?> {
    return dataSource.observeCurrentSession()
  }

  override suspend fun createSession(libraryItemId: LibraryItemId): Session {
    val startedAt = fatherTime.now()
    val libraryItem = libraryItemRepository.getLibraryItem(libraryItemId)

    val progress = libraryItem.userMediaProgress

    return dataSource.createOrStartSession(
      libraryItemId = libraryItemId,
      playMethod = PlayMethod.DirectPlay,
      mediaPlayer = "campfire",
      duration = libraryItem.media.durationInMillis.milliseconds,
      currentTime = progress?.currentTime?.seconds ?: 0.seconds,
      startedAt = startedAt,
    )
  }

  override suspend fun deleteSession(libraryItemId: LibraryItemId) {
    // Delete locally stored session for a given library item
    dataSource.deleteSession(libraryItemId)

    // Now clear its media progress
    mediaProgressDataSource.deleteMediaProgress(libraryItemId)

    // Now delete progress from server
    api.deleteMediaProgress(libraryItemId)
      .onSuccess {
        bark(LogPriority.INFO) { "Successfully deleted progress for $libraryItemId on server" }
      }
      .onFailure {
        bark(LogPriority.ERROR) { "Failed to delete media progress for $libraryItemId" }
      }
  }

  override suspend fun updateSession(libraryItemId: LibraryItemId, currentTime: Duration) {
    dataSource.updateSession(
      libraryItemId = libraryItemId,
      currentTime = currentTime,
    )
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId) {
    dataSource.stopSession(libraryItemId)
  }
}
