package app.campfire.sessions

import app.campfire.account.api.UserRepository
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.time.FatherTime
import app.campfire.sessions.api.SessionsRepository
import app.campfire.sessions.api.models.DeviceInfo
import app.campfire.sessions.api.models.PlayMethod
import app.campfire.sessions.api.models.Session
import app.campfire.sessions.api.models.SessionId
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultSessionsRepository(
  private val userRepository: UserRepository,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
) : SessionsRepository {

  private val sessionFlow = MutableStateFlow<Session?>(null)

  @OptIn(ExperimentalUuidApi::class)
  override suspend fun createSession(item: LibraryItem): Session {
    return Session(
      id = Uuid.random().toString(),
      libraryItem = item,
      playMethod = PlayMethod.Local,
      mediaPlayer = "exo-player",
      deviceInfo = DeviceInfo(
        id = Uuid.random().toString(),
        userId = "",
        deviceId = "",
        clientName = "Campfire",
        clientVersion = "0.0.1",
      ),
      duration = 15.hours,
      timeListening = 1.hours,
      startTime = 0.seconds,
      currentTime = 2.hours,
      startedAt = fatherTime.now(),
      updatedAt = fatherTime.now(),
    ).also {
      sessionFlow.value = it
    }
  }

  override suspend fun deleteSession(sessionId: SessionId) {
    sessionFlow.value = null
  }

  override fun observeCurrentSession(): Flow<Session?> {
    return sessionFlow.asStateFlow()
  }
}
