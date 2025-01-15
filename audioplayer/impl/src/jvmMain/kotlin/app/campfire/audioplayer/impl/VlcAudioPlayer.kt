package app.campfire.audioplayer.impl

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import app.campfire.audioplayer.impl.mediaitem.MediaItemBuilder
import app.campfire.audioplayer.impl.player.VlcPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.settings.PlaybackSettings
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.bark
import app.campfire.core.model.Session
import app.campfire.core.time.FatherTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class VlcAudioPlayer(
  private val settings: PlaybackSettings,
  private val fatherTime: FatherTime,
) : AudioPlayer {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val mediaPlayer: VlcPlayer = VlcPlayer().apply {
    setListener(VlcPlayerListener())
  }

  override var preparedSession: Session? = null

  override val state = MutableStateFlow(AudioPlayer.State.Disabled)
  override val overallTime = MutableStateFlow(0.seconds)
  override val currentTime = MutableStateFlow(0.seconds)
  override val currentDuration = MutableStateFlow(0.seconds)
  override val currentMetadata = MutableStateFlow(Metadata())
  override val playbackSpeed = MutableStateFlow(1f)
  override val runningTimer = MutableStateFlow<RunningTimer?>(null)

  private var playbackTimer: PlaybackTimer? = null
  private var playbackTimerJob: Job? = null

  override suspend fun prepare(
    session: Session,
    playImmediately: Boolean,
    chapterId: Int?,
  ) {
    preparedSession = session

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

    mediaPlayer.prepare(playImmediately, startTimeInChapterMs)
  }

  override fun release() {
    stop()
  }

  override fun pause() {
    mediaPlayer.pause()
  }

  override fun playPause() {
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
    clearTimer()
    playbackTimer = timer
    runningTimer.value = RunningTimer(timer, fatherTime.nowInEpochMillis())
    startTimer(timer)
  }

  private fun startTimer(timer: PlaybackTimer) {
    if (timer is PlaybackTimer.Epoch) {
      playbackTimerJob = scope.async {
        delay(timer.epochMillis)
        mediaPlayer.pause()
        clearTimer()
      }
    }
  }

  override fun clearTimer() {
    playbackTimerJob?.cancel()
    playbackTimerJob = null
    playbackTimer = null
    runningTimer.value = null
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
      var timerTriggered = false
      if (playbackTimer is PlaybackTimer.EndOfChapter) {
        mediaPlayer.pause()
        clearTimer()
        timerTriggered = true
      }
      currentMetadata.value = Metadata(
        title = mediaItem.metadata?.title,
        artworkUri = mediaItem.metadata?.artworkUri,
      )
      return timerTriggered
    }
  }
}
