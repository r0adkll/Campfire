package app.campfire.audioplayer.impl.session

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import app.campfire.sessions.api.SessionSynchronizer
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultPlaybackSessionManager(
  private val sessionsRepository: SessionsRepository,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val synchronizer: SessionSynchronizer,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
) : PlaybackSessionManager {

  private var updateJob: Job? = null
  private var synchronizerJob: Job? = null

  override suspend fun startSession(
    libraryItemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
  ) {
    withContext(dispatcherProvider.io) {
      val session = sessionsRepository.createSession(libraryItemId)

      bark("AudioPlayer") { "Preparing playback session: $session" }

      val player = audioPlayerHolder.currentPlayer.value
        ?: throw IllegalStateException("There isn't a media player available, unable to prepare session")
      player.prepare(session, playImmediately, chapterId)

      // Keep the local database session and progress updated with the player
      var lastNetworkSync = fatherTime.nowInEpochMillis()
      updateJob?.cancel()
      updateJob = player.overallTime
        .onEach { time ->
//          sessionsRepository.updateCurrentTime(libraryItemId, time)

          // Let's update the server for each [NetworkSyncInterval] to make sure things reflect semi-up-to-date
          // as a user is listening to an itemf
          // TODO: Make this smarter by checking if on a metered connection or not
          //  and dynamically adjust the sync interval based on such information.
          val elapsed = fatherTime.nowInEpochMillis() - lastNetworkSync
          if (elapsed > NetworkSyncInterval) {
            synchronizer.sync(libraryItemId)
            lastNetworkSync = fatherTime.nowInEpochMillis()
          }
        }
        .onCompletion {
          bark(LogPriority.INFO) { "Update Session Sync Completed" }
        }
        .launchIn(this)

      // Keep this session / progress synchronized with the server
      synchronizerJob?.cancel()
      synchronizerJob = player.state
        .onEach { state ->
          if (state == AudioPlayer.State.Paused || state == AudioPlayer.State.Disabled) {
            synchronizer.sync(libraryItemId)
          }
        }
        .onCompletion {
          bark(LogPriority.INFO) { "Player State Synchronize Completed" }
        }
        .launchIn(this)
    }
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId) {
    updateJob?.cancel()
    updateJob = null

    synchronizerJob?.cancel()
    synchronizerJob = null

    bark("AudioPlayer") { "Stopping playback session for $libraryItemId" }
    sessionsRepository.stopSession(libraryItemId)
  }
}

private const val NetworkSyncInterval = 60_000L // 1min
