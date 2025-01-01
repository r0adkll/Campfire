package app.campfire.sessions.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Cork
import app.campfire.core.model.Session
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
}

@Inject
@ContributesMultibinding(AppScope::class)
class LocalSessionUpdateSynchronizer(
  private val fatherTime: FatherTime,
) : PlaybackSynchronizer {

  // FIXME: This is a hack to get around the AppScope -> UserScope issue.
  //  We need to find a better way to organize our user management and scoping to prevent this
  //  when we DON'T really need the separation for some of these operations
  private val sessionsRepository: SessionsRepository
    get() = ComponentHolder.component<LocalSessionComponent>().sessionsRepository

  private var lastPlayedTime = mutableMapOf<String, Long>()

  override suspend fun onStateChanged(session: Session, state: AudioPlayer.State) {
    if (state == AudioPlayer.State.Playing) {
      lastPlayedTime[session.libraryItem.id] = fatherTime.nowInEpochMillis()
    } else if (state == AudioPlayer.State.Paused || state == AudioPlayer.State.Disabled) {
      val lastPlayed = lastPlayedTime[session.libraryItem.id]
      if (lastPlayed != null) {
        val elapsed = (fatherTime.nowInEpochMillis() - lastPlayed).milliseconds
        ibark { "Adding $elapsed time listening to ${session.libraryItem.id}" }
        sessionsRepository.addTimeListening(session.libraryItem.id, elapsed)
      }
    }
  }

  override suspend fun onOverallTimeChanged(session: Session, overallTime: Duration) {
    dbark { "onOverallTimeChanged(session=${session.id}, ${overallTime})" }
    sessionsRepository.updateCurrentTime(session.libraryItem.id, overallTime)
  }

  companion object : Cork {
    override val tag: String = LocalSessionUpdateSynchronizer::class.simpleName!!
  }
}
