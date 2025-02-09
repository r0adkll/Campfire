package app.campfire.audioplayer.impl

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import app.campfire.audioplayer.impl.mediaitem.MediaItemBuilder
import app.campfire.audioplayer.impl.player.VlcPlayer
import app.campfire.audioplayer.impl.sleep.SleepTimerManager
import app.campfire.audioplayer.impl.sleep.VolumeFadeController
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.bark
import app.campfire.core.model.Session
import app.campfire.settings.api.PlaybackSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VlcAudioPlayer(
  private val settings: PlaybackSettings,
  sleepTimerManagerFactory: SleepTimerManager.Factory,
) : AudioPlayer {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val mediaPlayer: VlcPlayer = VlcPlayer().apply {
    setListener(VlcPlayerListener())
  }

  private val sleepTimerManager = sleepTimerManagerFactory.create(this)

  override var preparedSession: Session? = null

  override val state = MutableStateFlow(AudioPlayer.State.Disabled)
  override val overallTime = MutableStateFlow(0.seconds)
  override val currentTime = MutableStateFlow(0.seconds)
  override val currentDuration = MutableStateFlow(0.seconds)
  override val currentMetadata = MutableStateFlow(Metadata())
  override val playbackSpeed = MutableStateFlow(1f)

  override val runningTimer: StateFlow<RunningTimer?>
    get() = sleepTimerManager.runningTimer

  private var fadeJob: Job? = null
  private var previousVolumeLevel: Float = 0f

  override suspend fun prepare(
    session: Session,
    playImmediately: Boolean,
    chapterId: Int?,
  ) {
    preparedSession = session
    state.value = AudioPlayer.State.Initializing

    // Build and set media items for the current session
    val mediaItems = MediaItemBuilder.build(session)
    mediaPlayer.setMediaItems(mediaItems)

    // Seek the media player
    var startTimeInChapterMs = 0L
    if (chapterId != null) {
      val chapter = session.libraryItem.media.chapters.find { it.id == chapterId }
        ?: error("Unable to find chapter to start")

      val overallProgressOfChapterMs = session.libraryItem.media.chapters.fold(0L) { acc, c ->
        if (c.id < chapterId) {
          acc + c.duration.inWholeMilliseconds
        } else {
          acc
        }
      }

      mediaPlayer.setCurrentItem(chapterId)

      bark {
        """
          Preparing VLC media player(
            chapter = $chapter,
            overallProgress = $overallProgressOfChapterMs,
            chapterId = $chapterId,
          )
        """.trimIndent()
      }

      currentTime.value = 0.seconds
      currentDuration.value = chapter.duration
      currentMetadata.value = Metadata(
        title = chapter.title,
        artworkUri = session.libraryItem.media.coverImageUrl,
      )
      overallTime.value = overallProgressOfChapterMs.milliseconds
    } else if (session.currentTime.isFinite() && session.currentTime > 0.seconds) {
      val chapter = session.chapter
      val progressInChapter = (session.currentTime - chapter.start.seconds)
      mediaPlayer.setCurrentItem(chapter.id)
      startTimeInChapterMs = progressInChapter.inWholeMilliseconds

      bark {
        """
          Preparing VLC media player(
            chapter = $chapter,
            progressInChapter = $progressInChapter,
            session-currentTime = ${session.currentTime.inWholeMilliseconds}
          )
        """.trimIndent()
      }

      // Hydrate the current states so the UI reflects appropriately
      currentTime.value = progressInChapter
      currentDuration.value = chapter.duration
      currentMetadata.value = Metadata(
        title = chapter.title,
        artworkUri = session.libraryItem.media.coverImageUrl,
      )
      overallTime.value = session.currentTime
    }

    if (playImmediately) {
      sleepTimerManager.onSessionStart()
    }

    mediaPlayer.prepare(playImmediately, startTimeInChapterMs)
  }

  override fun release() {
    stop()
  }

  override fun pause() {
    mediaPlayer.pause()
  }

  override fun fadeToPause(duration: Duration, tickRate: Long): Job {
    previousVolumeLevel = mediaPlayer.volume
    fadeJob?.cancel()

    return VolumeFadeController.fade(
      scope = scope,
      duration = duration,
      tickRate = tickRate,
      getVolume = { mediaPlayer.volume },
      setVolume = { mediaPlayer.volume = it },
      onPause = { mediaPlayer.pause() },
    ).also { fadeJob = it }
  }

  override fun playPause() {
    if (state.value == AudioPlayer.State.Paused) {
      sleepTimerManager.onSessionStart()
    }

    // Reset volume if stored
    if (mediaPlayer.volume == 0f && previousVolumeLevel > 0f) {
      mediaPlayer.volume = previousVolumeLevel
    } else if (mediaPlayer.volume == 0f) {
      mediaPlayer.volume = 1f
    }

    mediaPlayer.playPause()
  }

  override fun stop() {
    preparedSession = null
    mediaPlayer.stop()
    mediaPlayer.release()
    scope.cancel()
  }

  override fun seekTo(itemIndex: Int) {
    mediaPlayer.seekTo(itemIndex)
  }

  override fun seekTo(progress: Float) {
    mediaPlayer.seekTo(progress)
  }

  override fun seekTo(timestamp: Duration) {
    val timestampInMillis = timestamp.inWholeMilliseconds
    var mediaItemOffsetMs = 0L

    for (index in 0 until mediaPlayer.getMediaItemCount()) {
      val mediaItem = mediaPlayer.getMediaItemAt(index)
      val mediaItemDuration = mediaItem.metadata?.durationMs ?: error("Media Metadata Corrupted")
      val mediaItemEnd = mediaItemOffsetMs + mediaItemDuration
      if (timestampInMillis in mediaItemOffsetMs until mediaItemEnd) {
        val progressInMediaItem = timestampInMillis - mediaItemOffsetMs
        mediaPlayer.seekTo(index, progressInMediaItem)
        return
      }
      mediaItemOffsetMs = mediaItemEnd
    }
  }

  override fun skipToNext() {
    mediaPlayer.skipToNext()
  }

  override fun skipToPrevious() {
    if (currentTime.value > settings.trackResetThreshold) {
      mediaPlayer.seekTo(0f)
    } else {
      mediaPlayer.skipToPrevious()
    }
  }

  override fun seekForward() {
    mediaPlayer.seekForward(settings.forwardTimeMs)
  }

  override fun seekBackward() {
    mediaPlayer.seekBackward(settings.backwardTimeMs)
  }

  override fun setPlaybackSpeed(speed: Float) {
    playbackSpeed.value = speed
    mediaPlayer.setPlaybackSpeed(speed)
  }

  override fun setTimer(timer: PlaybackTimer) {
    sleepTimerManager.setTimer(timer)
  }

  override fun clearTimer() {
    sleepTimerManager.clearTimer()
  }

  private inner class VlcPlayerListener : VlcPlayer.Listener {
    override fun onStateChanged(state: AudioPlayer.State) {
      this@VlcAudioPlayer.state.value = state
    }

    override fun onDurationChanged(durationInMillis: Long) {
      currentDuration.value = durationInMillis.milliseconds
    }

    override fun onPositionChanged(positionInMillis: Long) {
      updateProgress(positionInMillis)
    }

    private fun updateProgress(currentPositionInMillis: Long) {
      currentTime.value = currentPositionInMillis.milliseconds

      var timelineOffsetMs = 0L
      val currentIndex = mediaPlayer.currentItemIndex
      (0 until currentIndex).forEach { index ->
        timelineOffsetMs += mediaPlayer.getMediaItemAt(index)
          .metadata
          ?.durationMs
          ?: 0L
      }

      overallTime.value = (timelineOffsetMs + currentPositionInMillis).milliseconds
    }

    override fun onMediaItemChanged(mediaItem: MediaItem): Boolean {
      val timerTriggered = sleepTimerManager.endOfChapter()
      currentMetadata.value = Metadata(
        title = mediaItem.metadata?.title,
        artworkUri = mediaItem.metadata?.artworkUri,
      )
      return timerTriggered
    }
  }
}
