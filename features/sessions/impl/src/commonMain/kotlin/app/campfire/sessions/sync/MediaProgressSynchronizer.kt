package app.campfire.sessions.sync

import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.asSeconds
import app.campfire.core.extensions.epochMilliseconds
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Session
import app.campfire.user.api.MediaProgressRepository
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlin.time.Duration
import me.tatarka.inject.annotations.Inject

@ContributesTo(UserScope::class)
interface MediaProgressSynchronizerComponent {
  val mediaProgressRepository: MediaProgressRepository
}

@ContributesMultibinding(AppScope::class)
@Inject
class MediaProgressSynchronizer : PlaybackSynchronizer {

  // We want this to process last in the list of synchronizers so other synchros
  // have the chance to update the local database with the latest information.
  override val rank: Int = PlaybackSynchronizer.RANK_HIGHEST

  // FIXME: This is a hack to get around the AppScope -> UserScope issue.
  //  We need to find a better way to organize our user management and scoping to prevent this
  //  when we DON'T really need the separation for some of these operations
  private val mediaProgressRepository: MediaProgressRepository
    get() = ComponentHolder.component<MediaProgressSynchronizerComponent>().mediaProgressRepository

  override suspend fun onOverallTimeChanged(session: Session, overallTime: Duration) {
    val updatedProgress = MediaProgress(
      userId = session.userId,
      libraryItemId = session.libraryItem.id,
      episodeId = null,
      mediaItemId = session.libraryItem.media.id,
      mediaItemType = session.libraryItem.mediaType,
      duration = session.libraryItem.media.durationInSeconds,
      progress = session.progress,
      currentTime = session.currentTime.asSeconds(),
      isFinished = session.isFinished,
      hideFromContinueListening = false,
      ebookLocation = null,
      ebookProgress = null,
      finishedAt = if (session.isFinished) {
        session.updatedAt.epochMilliseconds
      } else {
        null
      },
      lastUpdate = session.updatedAt.epochMilliseconds,
      startedAt = session.startedAt.epochMilliseconds,
    )

    mediaProgressRepository.updateProgress(updatedProgress)
  }
}
