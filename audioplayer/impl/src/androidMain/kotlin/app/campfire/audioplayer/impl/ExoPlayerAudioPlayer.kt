package app.campfire.audioplayer.impl

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_IS_PLAYING_CHANGED
import androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION
import androidx.media3.common.Player.EVENT_PLAYBACK_STATE_CHANGED
import androidx.media3.common.Player.EVENT_PLAY_WHEN_READY_CHANGED
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.impl.util.AUDIO_TAG
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.settings.PlaybackSettings
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.LogPriority
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@OptIn(UnstableApi::class)
class ExoPlayerAudioPlayer(
  private val context: Context,
  private val settings: PlaybackSettings,
  private val fatherTime: FatherTime,
  private val mediaSourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(context),
) : AudioPlayer, Player.Listener {

  @Inject
  class Factory(
    private val settings: PlaybackSettings,
    private val mediaSourceFactory: MediaSource.Factory,
    private val fatherTime: FatherTime,
  ) {

    fun create(context: Context): ExoPlayerAudioPlayer {
      return ExoPlayerAudioPlayer(
        context = context,
        settings = settings,
        mediaSourceFactory = mediaSourceFactory,
        fatherTime = fatherTime,
      )
    }
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  internal val exoPlayer = ExoPlayer.Builder(context)
    .setSeekForwardIncrementMs(settings.forwardTimeMs)
    .setSeekBackIncrementMs(settings.backwardTimeMs)
    .setHandleAudioBecomingNoisy(true)
    .setLoadControl(
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          20 * 1000,
          45 * 1000,
          5 * 1000,
          20 * 1000,
        )
        .build(),
    )
    .setBandwidthMeter(
      DefaultBandwidthMeter.Builder(context)
        .build(),
    )
    .setMediaSourceFactory(mediaSourceFactory)
    .build()
    .apply {
      addListener(this@ExoPlayerAudioPlayer)
    }

  private var progressJob: Job? = null

  private var playbackTimer: PlaybackTimer? = null
  private var playbackTimerJob: Job? = null

  override var preparedSession: Session? = null

  override val state = MutableStateFlow(AudioPlayer.State.Disabled)
  override val overallTime = MutableStateFlow(0.seconds)
  override val currentTime = MutableStateFlow(0.seconds)
  override val currentDuration = MutableStateFlow(0.seconds)
  override val currentMetadata = MutableStateFlow(Metadata())
  override val playbackSpeed = MutableStateFlow(1f)
  override val runningTimer = MutableStateFlow<RunningTimer?>(null)

  override suspend fun prepare(
    session: Session,
    playImmediately: Boolean,
    chapterId: Int?,
  ) = withContext(Dispatchers.Main) {
    preparedSession = session

    val mediaItems = MediaItemBuilder.build(session)

    bark(AUDIO_TAG, LogPriority.INFO) {
      """
        Prepare Session(
          itemId = ${session.libraryItem.id},
          title = ${session.libraryItem.media.metadata.title},
          playMethod = ${session.playMethod},
          mediaPlayer = ${session.mediaPlayer},
          currentTime = ${session.currentTime},
          chapterId = $chapterId
        )
      """.trimIndent()
    }

    exoPlayer.run {
      // Set the media list
      setMediaItems(mediaItems, true)

      // Seek the media player
      if (chapterId != null) {
        // If the Chapter Id is passed explicitly then we can take that intention as
        // starting playback directly at that chapter
        val chapter = session.libraryItem.media.chapters.find { it.id == chapterId }
          ?: error("Unable to find chapter to start")

        val overallProgressOfChapterMs = session.libraryItem.media.chapters.fold(0L) { acc, c ->
          if (c.id < chapterId) {
            acc + c.duration.inWholeMilliseconds
          } else {
            acc
          }
        }

        seekTo(chapterId)
        currentTime.value = 0.seconds
        currentDuration.value = chapter.duration
        currentMetadata.value = Metadata(
          title = chapter.title,
          artworkUri = session.libraryItem.media.coverImageUrl,
        )
        overallTime.value = overallProgressOfChapterMs.milliseconds
      } else if (session.currentTime.isFinite() && session.currentTime > 0.seconds) {
        val chapter = session.chapter
        val progressInChapterMs = (session.currentTime - chapter.start.seconds)
          .inWholeMilliseconds.coerceAtLeast(0L)
        seekTo(chapter.id, progressInChapterMs)

        // Hydrate the current states so the UI reflects appropriately
        currentTime.value = progressInChapterMs.milliseconds
        currentDuration.value = chapter.duration
        currentMetadata.value = Metadata(
          title = chapter.title,
          artworkUri = session.libraryItem.media.coverImageUrl,
        )
        overallTime.value = session.currentTime
      }

      // Set when to play, and prepare
      playWhenReady = playImmediately
      prepare()
    }
  }

  override fun release() {
    preparedSession = null
    scope.cancel()
  }

  override fun pause() {
    exoPlayer.pause()
  }

  override fun playPause() {
    if (exoPlayer.isPlaying) {
      exoPlayer.pause()
    } else {
      exoPlayer.play()
    }
  }

  override fun stop() {
    preparedSession = null
    exoPlayer.stop()
  }

  override fun seekTo(itemIndex: Int) {
    exoPlayer.seekToDefaultPosition(itemIndex)
    exoPlayer.play()
  }

  override fun seekTo(progress: Float) {
    val positionMs = (progress * exoPlayer.duration).toLong()
    exoPlayer.seekTo(positionMs)
    currentTime.value = positionMs.milliseconds
  }

  override fun seekTo(timestamp: Duration) {
    val timestampInMillis = timestamp.inWholeMilliseconds
    var mediaItemOffsetMs = 0L

    for (index in 0 until exoPlayer.mediaItemCount) {
      val mediaItem = exoPlayer.getMediaItemAt(index)
      val mediaItemDuration = mediaItem.mediaMetadata.durationMs ?: error("Media Metadata Corrupted")
      val mediaItemEnd = mediaItemOffsetMs + mediaItemDuration
      if (timestampInMillis in mediaItemOffsetMs until mediaItemEnd) {
        val progressInMediaItem = timestampInMillis - mediaItemOffsetMs
        exoPlayer.seekTo(index, progressInMediaItem)
        exoPlayer.play()
        return
      }
      mediaItemOffsetMs = mediaItemEnd
    }
  }

  override fun skipToNext() {
    exoPlayer.seekToNextMediaItem()
  }

  override fun skipToPrevious() {
    exoPlayer.seekToPreviousMediaItem()
  }

  override fun seekForward() {
    exoPlayer.seekForward()
  }

  override fun seekBackward() {
    exoPlayer.seekBack()
  }

  override fun setPlaybackSpeed(speed: Float) {
    playbackSpeed.value = speed
    exoPlayer.setPlaybackSpeed(speed)
  }

  override fun setTimer(timer: PlaybackTimer) {
    clearTimer()
    playbackTimer = timer
    runningTimer.value = RunningTimer(timer, fatherTime.nowInEpochMillis())
    startTimer(timer)
  }

  private fun startTimer(timer: PlaybackTimer) {
    // TODO: abstract this logic?
    if (timer is PlaybackTimer.Epoch) {
      playbackTimerJob = scope.async {
        delay(timer.epochMillis)
        exoPlayer.pause()
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

  /*
   * Player Listener Callbacks
   */

  override fun onTimelineChanged(timeline: Timeline, reason: Int) {
    currentDuration.value = exoPlayer.duration.milliseconds
  }

  override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
    currentDuration.value = exoPlayer.duration.milliseconds
    currentMetadata.value = Metadata(
      title = mediaMetadata.title?.toString(),
      artworkUri = mediaMetadata.artworkUri?.toString(),
    )
  }

  override fun onEvents(player: Player, events: Player.Events) {
    if (events.containsAny(
        EVENT_PLAYBACK_STATE_CHANGED,
        EVENT_PLAY_WHEN_READY_CHANGED,
        EVENT_IS_PLAYING_CHANGED,
      )
    ) {
      state.value = when (player.playbackState) {
        Player.STATE_BUFFERING -> AudioPlayer.State.Buffering
        Player.STATE_READY -> when (player.isPlaying) {
          true -> AudioPlayer.State.Playing
          false -> AudioPlayer.State.Paused
        }

        else -> AudioPlayer.State.Disabled
      }

      if (player.isPlaying) {
        observeProgress(player)
      } else {
        progressJob?.cancel()
      }
    }

    // If the media item transitions (i.e. chapter) and the timer is
    // end of chapter, then stop the playback
    if (events.containsAny(EVENT_MEDIA_ITEM_TRANSITION)) {
      if (playbackTimer is PlaybackTimer.EndOfChapter) {
        player.pause()
        clearTimer()
      }
    }
  }

  private fun observeProgress(player: Player) {
    progressJob?.cancel()
    progressJob = scope.launch {
      bark(AUDIO_TAG) { "Starting Progress Observer" }
      while (isActive) {
        updateProgress(player)
        delay(500L)
      }
    }
    progressJob?.invokeOnCompletion {
      bark(AUDIO_TAG) { "Finished Progress Observer" }
    }
  }

  private fun updateProgress(player: Player) {
    currentTime.value = player.currentPosition.milliseconds
    currentDuration.value = player.duration.milliseconds

    var timelineOffsetMs = 0L
    val currentIndex = player.currentMediaItemIndex
    (0 until currentIndex).forEach { index ->
      timelineOffsetMs += player.getMediaItemAt(index)
        .mediaMetadata
        .durationMs
        ?: 0L
    }

    overallTime.value = (timelineOffsetMs + player.currentPosition).milliseconds
  }
}
