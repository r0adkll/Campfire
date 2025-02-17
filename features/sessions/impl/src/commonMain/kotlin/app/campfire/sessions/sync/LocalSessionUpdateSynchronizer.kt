package app.campfire.sessions.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import me.tatarka.inject.annotations.Inject

@ContributesTo(UserScope::class)
interface LocalSessionComponent {
  val sessionsRepository: SessionsRepository
  val remoteSessionsUpdater: RemoteSessionsUpdater
}

@Inject
@ContributesMultibinding(AppScope::class)
class LocalSessionUpdateSynchronizer(
  private val fatherTime: FatherTime,
) : PlaybackSynchronizer {

  // FIXME: This is a hack to get around the AppScope -> UserScope issue.
  //  We need to find a better way to organize our user management and scoping to prevent this
  //  when we DON'T really need the separation for some of these operations
  private val component: LocalSessionComponent
    get() = ComponentHolder.component<LocalSessionComponent>()

  private var lastPlayedTime = mutableMapOf<String, Long>()

  override suspend fun onStateChanged(
    libraryItemId: LibraryItemId,
    state: AudioPlayer.State,
    previousState: AudioPlayer.State,
  ) {
    if (state == AudioPlayer.State.Playing) {
      lastPlayedTime[libraryItemId] = fatherTime.nowInEpochMillis()
    } else if (state == AudioPlayer.State.Paused || state == AudioPlayer.State.Disabled) {
      val lastPlayed = lastPlayedTime[libraryItemId]
      if (lastPlayed != null) {
        val elapsed = (fatherTime.nowInEpochMillis() - lastPlayed).milliseconds
        ibark { "Adding $elapsed time listening to $libraryItemId" }
        component.sessionsRepository.addTimeListening(libraryItemId, elapsed)
        component.remoteSessionsUpdater.update(skipInterval = true)
      }
    }
  }

  override suspend fun onOverallTimeChanged(libraryItemId: LibraryItemId, overallTime: Duration) {
    component.sessionsRepository.updateCurrentTime(libraryItemId, overallTime)

//    // Add time since last played to the current session
//    val lastPlayed = lastPlayedTime[libraryItemId]
//    val now = fatherTime.nowInEpochMillis()
//    lastPlayedTime[libraryItemId] = now
//    if (lastPlayed != null) {
//      val elapsed = (now - lastPlayed).milliseconds
//      component.sessionsRepository.addTimeListening(libraryItemId, elapsed)
//    }

    // Trigger an update if conditions are right
    component.remoteSessionsUpdater.update()
  }

  companion object : Cork {
    override val tag: String = LocalSessionUpdateSynchronizer::class.simpleName!!
  }
}
