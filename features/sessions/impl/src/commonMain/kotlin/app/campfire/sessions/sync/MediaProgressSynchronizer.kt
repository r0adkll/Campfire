package app.campfire.sessions.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.asSeconds
import app.campfire.core.extensions.epochMilliseconds
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.sessions.api.SessionsRepository
import app.campfire.user.api.MediaProgressRepository
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlin.time.Duration
import kotlin.uuid.Uuid
import me.tatarka.inject.annotations.Inject

@ContributesTo(UserScope::class)
interface MediaProgressSynchronizerComponent {
  val sessionsRepository: SessionsRepository
}

@ContributesMultibinding(AppScope::class)
@Inject
class MediaProgressSynchronizer(
  private val mediaProgressRepository: MediaProgressRepository,
) : PlaybackSynchronizer {

  private val sessionsRepository: SessionsRepository
    get() = ComponentHolder.component<MediaProgressSynchronizerComponent>().sessionsRepository

  // We want this to process last in the list of synchronizers so other synchros
  // have the chance to update the local database with the latest information.
  override val rank: Int = PlaybackSynchronizer.RANK_HIGHEST

  override suspend fun onOverallTimeChanged(libraryItemId: LibraryItemId, overallTime: Duration) {
    syncProgress(libraryItemId)
  }

  override suspend fun onStateChanged(
    sessionId: Uuid,
    libraryItemId: LibraryItemId,
    state: AudioPlayer.State,
    previousState: AudioPlayer.State,
  ) {
    if (state == AudioPlayer.State.Paused && previousState == AudioPlayer.State.Playing) {
      syncProgress(libraryItemId, force = true)
    }
  }

  private suspend fun syncProgress(libraryItemId: LibraryItemId, force: Boolean = false) {
    val session = sessionsRepository.getSession(libraryItemId) ?: return

    val updatedProgress = MediaProgress(
      id = MediaProgress.UNKNOWN_ID,
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

    mediaProgressRepository.updateProgress(updatedProgress, force)
  }
}
