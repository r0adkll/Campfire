package app.campfire.sessions

import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.seconds
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.Session
import app.campfire.core.time.FatherTime
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.sessions.api.SessionsRepository
import app.campfire.sessions.db.SessionDataSource
import app.campfire.user.api.MediaProgressRepository
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
  private val mediaProgressRepository: MediaProgressRepository,
  private val dataSource: SessionDataSource,
) : SessionsRepository {

  override fun observeCurrentSession(): Flow<Session?> {
    return dataSource.observeCurrentSession()
  }

  override suspend fun getSession(libraryItemId: LibraryItemId): Session? {
    return dataSource.getSession(libraryItemId)
  }

  override suspend fun createSession(libraryItemId: LibraryItemId): Session {
    val startedAt = fatherTime.now()
    val libraryItem = libraryItemRepository.getLibraryItem(libraryItemId)
    val progress = mediaProgressRepository.getProgress(libraryItemId)

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
    dataSource.deleteSession(libraryItemId)
  }

  override suspend fun updateCurrentTime(libraryItemId: LibraryItemId, currentTime: Duration) {
    dataSource.updateCurrentTime(
      libraryItemId = libraryItemId,
      currentTime = currentTime,
    )
  }

  override suspend fun addTimeListening(libraryItemId: LibraryItemId, amount: Duration) {
    dataSource.addTimeListening(
      libraryItemId = libraryItemId,
      amount = amount,
    )
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId) {
    dataSource.stopSession(libraryItemId)
  }
}
