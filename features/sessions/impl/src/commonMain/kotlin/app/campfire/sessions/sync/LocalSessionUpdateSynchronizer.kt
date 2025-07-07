package app.campfire.sessions.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid
import me.tatarka.inject.annotations.Inject

@ContributesTo(UserScope::class)
interface LocalSessionComponent {
  val sessionsRepository: SessionsRepository
  val remoteSessionsUpdater: RemoteSessionsUpdater
}

@Inject
@SingleIn(AppScope::class)
@ContributesMultibinding(AppScope::class)
class LocalSessionUpdateSynchronizer(
  private val fatherTime: FatherTime,
) : PlaybackSynchronizer {

  // FIXME: This is a hack to get around the AppScope -> UserScope issue.
  //  We need to find a better way to organize our user management and scoping to prevent this
  //  when we DON'T really need the separation for some of these operations
  private val component: LocalSessionComponent
    get() = ComponentHolder.component<LocalSessionComponent>()

  private var lastPlayedTime: Long? = null

  override suspend fun onStateChanged(
    sessionId: Uuid,
    libraryItemId: LibraryItemId,
    state: AudioPlayer.State,
    previousState: AudioPlayer.State,
  ) {
    if (state == AudioPlayer.State.Playing) {
      lastPlayedTime = fatherTime.nowInEpochMillis()
      ibark { "Setting lastPlayedTime to $lastPlayedTime for $libraryItemId" }
    } else if (
      state == AudioPlayer.State.Paused ||
      state == AudioPlayer.State.Disabled ||
      state == AudioPlayer.State.Finished
    ) {
      if (lastPlayedTime != null) {
        val elapsed = (fatherTime.nowInEpochMillis() - lastPlayedTime!!).milliseconds
        ibark { "Adding $elapsed time listening to $libraryItemId for ${sessionId.toHexDashString()})" }
        component.sessionsRepository.addTimeListening(libraryItemId, elapsed)
        component.remoteSessionsUpdater.update(skipInterval = true)
        lastPlayedTime = null
      }
    }
  }

  override suspend fun onOverallTimeChanged(libraryItemId: LibraryItemId, overallTime: Duration) {
    component.sessionsRepository.updateCurrentTime(libraryItemId, overallTime)

    // Check if its been too long since we synced listening time
    if (lastPlayedTime != null) {
      val elapsed = (fatherTime.nowInEpochMillis() - lastPlayedTime!!).milliseconds
      if (elapsed > MAX_TIME_LISTENING_INTERVAL) {
        ibark { "Timeout adding $elapsed time listening to $libraryItemId)" }
        component.sessionsRepository.addTimeListening(libraryItemId, elapsed)
        lastPlayedTime = fatherTime.nowInEpochMillis()
      }
    }

    // Trigger an update if conditions are right
    component.remoteSessionsUpdater.update()
  }

  companion object : Cork {
    override val tag: String = LocalSessionUpdateSynchronizer::class.simpleName!!

    private val MAX_TIME_LISTENING_INTERVAL = 1.minutes
  }
}
