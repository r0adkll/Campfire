// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_DEVICE_INFO_CHANGED
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
import app.campfire.audioplayer.OnFinishedListener
import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.audioplayer.impl.chapters.ChapterTimeline
import app.campfire.audioplayer.impl.forwarding.ChapterWindowForwardingPlayer
import app.campfire.audioplayer.impl.forwarding.PlaybackHistoryForwardingPlayer
import app.campfire.audioplayer.impl.forwarding.RemoteControlForwardingPlayer
import app.campfire.audioplayer.impl.mediaitem.MediaItemBuilder
import app.campfire.audioplayer.impl.sleep.SleepTimerManager
import app.campfire.audioplayer.impl.sleep.VolumeFadeController
import app.campfire.audioplayer.impl.util.AUDIO_TAG
import app.campfire.audioplayer.impl.util.eventAsDebugLog
import app.campfire.audioplayer.impl.util.playbackStateAsDebugLog
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.Cork
import app.campfire.core.logging.Corked
import app.campfire.core.model.Chapter
import app.campfire.core.model.Session
import app.campfire.core.model.loggableId
import app.campfire.core.toast.GlobalToaster
import app.campfire.core.toast.Toast
import app.campfire.crashreporting.CrashReporter
import app.campfire.infra.audioplayer.impl.R
import app.campfire.settings.api.PlaybackSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject

// The remote cast player surfaces no PlaybackException for receiver-side failures, so dead
// handoffs and vanished receivers are detected by deadline instead (see armCastWatchdog).
private const val CAST_HANDOFF_READY_TIMEOUT_MS = 20_000L
private const val CAST_STALL_TIMEOUT_MS = 30_000L
private const val CAST_FALLBACK_SWAP_TIMEOUT_MS = 5_000L

@OptIn(UnstableApi::class)
class ExoPlayerAudioPlayer(
  private val context: Context,
  private val settings: PlaybackSettings,
  private val sleepTimerManagerFactory: SleepTimerManager.Factory,
  private val playbackHistoryRecorder: PlaybackHistoryRecorder,
  private val castController: CastController,
  private val mediaSourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(context),
  private val remotePlayerFactory: RemotePlayerFactory = RemotePlayerFactory.NoOp,
) : AudioPlayer, Player.Listener, Cork {

  override val tag: String = AUDIO_TAG
  override val enabled: Boolean = true

  // Re-enable to emit verbose logging around Player.Listener events for
  // debugging.
  private val eventLogger = Corked("Player.Listener.Event", enabled = false)

  @Inject
  class Factory(
    private val settings: PlaybackSettings,
    private val mediaSourceFactory: MediaSource.Factory,
    private val sleepTimerManagerFactory: SleepTimerManager.Factory,
    private val playbackHistoryRecorder: PlaybackHistoryRecorder,
    private val castController: CastController,
    // Defaults to NoOp when the optional :infra:audioplayer:cast module isn't in this build.
    private val remotePlayerFactory: RemotePlayerFactory = RemotePlayerFactory.NoOp,
  ) {

    fun create(context: Context): ExoPlayerAudioPlayer {
      return ExoPlayerAudioPlayer(
        context = context,
        settings = settings,
        mediaSourceFactory = mediaSourceFactory,
        playbackHistoryRecorder = playbackHistoryRecorder,
        sleepTimerManagerFactory = sleepTimerManagerFactory,
        castController = castController,
        remotePlayerFactory = remotePlayerFactory,
      )
    }
  }

  private val sleepTimerManager = sleepTimerManagerFactory.create(this)

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val exoPlayer = ExoPlayer.Builder(context)
    .setRenderersFactory(CampfireRenderersFactory(context))
    .setSeekForwardIncrementMs(settings.forwardTimeMs)
    .setSeekBackIncrementMs(settings.backwardTimeMs)
    .setHandleAudioBecomingNoisy(true)
    .setAudioAttributes(
      AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
        .setUsage(C.USAGE_MEDIA)
        .build(),
      true,
    )
    .setLoadControl(
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          15 * 1000, // minBuffer
          30 * 1000, // maxBuffer (was 45s)
          2 * 1000, // playbackBuffer
          10 * 1000, // backBuffer (was 20s)
        )
        .build(),
    )
    .setBandwidthMeter(
      DefaultBandwidthMeter.Builder(context)
        .build(),
    )
    .setMediaSourceFactory(mediaSourceFactory)
    .build()
    .also { AudioPlayerDebugHooks.Holder.hooks.onExoPlayerCreated(it) }

  private val remotePlayer: Player? = remotePlayerFactory.create(context, exoPlayer)

  /**
   * A proxy accessor for the correct player client. All listener and access goes through the
   * [remotePlayer] (a CastPlayer when the cast module is present) which configures the
   * [exoPlayer] as the local player to use when a MediaRoute is not directed at remote
   * playback.
   */
  internal val internalPlayer: Player = (remotePlayer ?: exoPlayer).apply {
    addListener(this@ExoPlayerAudioPlayer)
  }

  /**
   * A wrapped version of the player that intercepts next/previous commands based on user settings.
   * This should be used for MediaSession to handle remote control commands differently.
   */
  private val remoteControlForwardingPlayer = RemoteControlForwardingPlayer(
    player = internalPlayer,
    settings = settings,
    appPackageName = context.packageName,
  )

  /**
   * A wrapped version of the player that intercepts and records playback commands
   * for the user
   */
  private val playbackHistoryForwardingPlayer = PlaybackHistoryForwardingPlayer(
    player = remoteControlForwardingPlayer,
    playbackSettings = settings,
    recorder = playbackHistoryRecorder,
    session = { preparedSession },
  )

  internal val player: Player get() = playbackHistoryForwardingPlayer

  /**
   * The player handed to the MediaSession. On top of the forwarding chain it projects
   * coarse single-item (HLS) playback as a virtual chapter playlist, so controller
   * consumers (media notification, Android Auto, external controllers) get the same
   * chapter-granular scrubber, titles, and next/prev semantics as the in-app UI.
   * Transparent for every other queue shape. In-app code keeps using [player] — its
   * positions and seeks are absolute.
   */
  internal val sessionPlayer: ChapterWindowForwardingPlayer = ChapterWindowForwardingPlayer(
    player = playbackHistoryForwardingPlayer,
    settings = settings,
    appPackageName = context.packageName,
    host = object : ChapterWindowForwardingPlayer.Host {
      override fun activeChapters(): List<Chapter>? = chapterTimeline
        ?.takeIf { queueShape == QueueShape.SINGLE && it.hasChapters }
        ?.chapters

      override fun seekToAbsolute(target: Duration) = seekTo(target, play = false)

      override fun skipToNextChapter() = skipToNext()

      override fun skipToPreviousChapter() = skipToPrevious()
    },
  )

  /**
   * Binds the MediaSession to the forwarding players so they can identify the source of
   * commands (remote controllers vs. notification/Auto) and apply user settings.
   *
   * Must be called after the MediaSession is created.
   */
  internal fun bindSession(session: androidx.media3.session.MediaSession) {
    remoteControlForwardingPlayer.session = session
    sessionPlayer.session = session
  }

  private var progressJob: Job? = null
  private var fadeJob: Job? = null
  private var previousVolumeLevel: Float = 0f
  private var isRemotePlayback = false
  private var castWatchdogJob: Job? = null
  private var chapterTimeline: ChapterTimeline? = null
  private var lastBoundaryCheckTime: Duration? = null
  private var hlsQueueActive = false
  private var hlsFallbackAttempted = false

  /**
   * How the current queue's media items map onto the book:
   * - [QueueShape.CHAPTERS] — one (possibly clipped) item per chapter; item boundaries ARE
   *   chapter boundaries, so player-native transitions/seeks carry chapter semantics.
   * - [QueueShape.TRACKS] — one item per whole file (remote/Cast: receivers don't honor
   *   clipping); chapter semantics are re-derived from the absolute position.
   * - [QueueShape.SINGLE] — one HLS stream item spanning the whole book; every position is
   *   already absolute.
   * Cast takes precedence: handing off mid-HLS rebuilds the queue per-track.
   */
  private val queueShape: QueueShape
    get() = when {
      isRemotePlayback -> QueueShape.TRACKS
      hlsQueueActive -> QueueShape.SINGLE
      else -> QueueShape.CHAPTERS
    }

  private enum class QueueShape { CHAPTERS, TRACKS, SINGLE }

  override var preparedSession: Session? = null
  private var finishedListener: OnFinishedListener? = null

  override val state = MutableStateFlow(AudioPlayer.State.Disabled)

  private val _error = MutableStateFlow<Throwable?>(null)
  override val error: StateFlow<Throwable?> = _error
  override val overallTime = MutableStateFlow(0.seconds)
  override val currentTime = MutableStateFlow(0.seconds)
  override val currentDuration = MutableStateFlow(0.seconds)
  override val currentMetadata = MutableStateFlow(Metadata())
  override val playbackSpeed = MutableStateFlow(settings.playbackSpeed)

  override val runningTimer: StateFlow<RunningTimer?>
    get() = sleepTimerManager.runningTimer

  override suspend fun prepare(
    session: Session,
    playImmediately: Boolean,
    chapterId: Int?,
    onFinished: OnFinishedListener,
  ) = withContext(Dispatchers.Main) {
    preparedSession = session
    chapterTimeline = ChapterTimeline(session)
    lastBoundaryCheckTime = null
    hlsQueueActive = session.episode == null && session.hlsStreamUrl != null
    hlsFallbackAttempted = false
    finishedListener = onFinished
    playbackSpeed.value = settings.playbackSpeedFor(session.libraryItem.id)
    _error.value = null
    state.value = AudioPlayer.State.Initializing

    val mediaItems = MediaItemBuilder.build(session).asPlatformMediaItems(context)

    ibark {
      """
        Prepare Session(
          itemId = ${session.libraryItem.id.loggableId},
          playMethod = ${session.playMethod},
          mediaPlayer = ${session.mediaPlayer},
          currentTime = ${session.currentTime},
          chapterId = $chapterId,
          episodeId = ${session.episodeId},
        )
      """.trimIndent()
    }

    val podcastEpisode = session.episode

    player.run {
      // Set the media list
      setMediaItems(mediaItems, true)

      // Set the playback speed
      setPlaybackSpeed(playbackSpeed.value)

      // Seek the media player
      if (podcastEpisode != null) {
        // Podcast episodes are a single MediaItem at index 0; chapter/track logic
        // doesn't apply. Seek to the resume position within the episode if any.
        val resumeMs = session.currentTime
          .takeIf { it.isFinite() && it > 0.seconds }
          ?.inWholeMilliseconds
          ?: 0L
        seekTo(0, resumeMs)
        currentTime.value = resumeMs.milliseconds
        currentDuration.value = podcastEpisode.duration
        currentMetadata.value = Metadata(
          title = podcastEpisode.title,
          artworkUri = session.libraryItem.media.coverImageUrl,
        )
        overallTime.value = resumeMs.milliseconds
      } else if (hlsQueueActive) {
        // Single stream item: every position is absolute, so both an explicit chapter and
        // a resume point reduce to one absolute seek on item 0.
        val resume = when {
          chapterId != null ->
            session.libraryItem.media.chapters.find { it.id == chapterId }?.start?.seconds
              ?: session.currentTime
          else -> session.currentTime
        }.takeIf { it.isFinite() && it > 0.seconds } ?: 0.seconds

        seekTo(0, resume.inWholeMilliseconds)
        overallTime.value = resume
        lastBoundaryCheckTime = resume

        val progress = chapterTimeline?.progressAt(resume)
        if (progress != null) {
          currentTime.value = progress.position
          currentDuration.value = progress.duration
          currentMetadata.value = Metadata(
            title = progress.chapter.title,
            artworkUri = session.libraryItem.media.coverImageUrl,
          )
        } else {
          currentTime.value = resume
          currentDuration.value = session.duration
          currentMetadata.value = Metadata(
            title = session.libraryItem.media.metadata.title,
            artworkUri = session.libraryItem.media.coverImageUrl,
          )
        }
      } else if (chapterId != null) {
        if (session.libraryItem.media.chapters.isNotEmpty()) {
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
        } else if (session.libraryItem.media.tracks.isNotEmpty()) {
          // If the Chapter Id is passed explicitly then we can take that intention as
          // starting playback directly at that chapter
          val track = session.libraryItem.media.tracks.find { it.index == chapterId }
            ?: error("Unable to find audio track to start")

          seekTo(chapterId)
          currentTime.value = 0.seconds
          currentDuration.value = track.duration.seconds
          currentMetadata.value = Metadata(
            title = track.taggedTitle,
            artworkUri = session.libraryItem.media.coverImageUrl,
          )
          overallTime.value = track.startOffset.seconds
        } else {
          CrashReporter.record(
            InvalidPlaybackSessionException(
              session,
              "Chapter/Track Prepare Failed: No Chapters / Tracks",
            ),
          )
        }
      } else if (session.currentTime.isFinite() && session.currentTime > 0.seconds) {
        val chapter = session.chapter
        val track = session.audioTrack
        if (chapter != null) {
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
        } else if (track != null) {
          val progressInTrackMs = (session.currentTime - track.startOffset.seconds)
            .inWholeMilliseconds.coerceAtLeast(0L)
          // AudioTrack indexes start at 1
          seekTo(track.index - 1, progressInTrackMs)

          // Hydrate the current states so the UI reflects appropriately
          currentTime.value = progressInTrackMs.milliseconds
          currentDuration.value = track.duration.seconds
          currentMetadata.value = Metadata(
            title = track.taggedTitle,
            artworkUri = session.libraryItem.media.coverImageUrl,
          )
          overallTime.value = session.currentTime
        } else {
          CrashReporter.record(
            InvalidPlaybackSessionException(
              session,
              "Session Time is > 0, unable to find chapter/track info",
            ),
          )
        }
      }

      if (playImmediately) {
        // Potentially trigger the auto sleep timer
        sleepTimerManager.onSessionStart()
      }

      // Set when to play, and prepare
      playWhenReady = playImmediately
      prepare()
    }
  }

  override fun release() {
    preparedSession = null
    finishedListener = null
    scope.cancel()
  }

  override fun pause() {
    player.pause()
  }

  override fun fadeToPause(duration: Duration, tickRate: Long): Job {
    previousVolumeLevel = player.volume
    fadeJob?.cancel()

    return VolumeFadeController.fade(
      scope = scope,
      duration = duration,
      tickRate = tickRate,
      getVolume = { player.volume },
      setVolume = { player.volume = it },
      onPause = { player.pause() },
    ).also { fadeJob = it }
  }

  override fun playPause() {
    if (player.isPlaying) {
      player.pause()
    } else {
      // Potentially trigger the auto sleep timer
      sleepTimerManager.onSessionStart()

      // Reset volume if stored
      if (player.volume == 0f && previousVolumeLevel > 0f) {
        player.volume = previousVolumeLevel
      } else if (player.volume == 0f) {
        player.volume = 1f
      }

      player.play()
    }
  }

  override fun stop() {
    preparedSession = null
    chapterTimeline = null
    lastBoundaryCheckTime = null
    hlsQueueActive = false
    hlsFallbackAttempted = false
    finishedListener = null
    player.stop()
  }

  override fun seekTo(itemIndex: Int) {
    if (queueShape != QueueShape.CHAPTERS) {
      val absolute = chapterTimeline?.startOfLocalQueueIndex(itemIndex)
      if (absolute != null) {
        coarseSeekTo(absolute, play = true)
        return
      }
    }
    player.seekToDefaultPosition(itemIndex)
    player.play()
  }

  override fun seekTo(progress: Float) {
    if (queueShape != QueueShape.CHAPTERS) {
      val chapter = chapterTimeline?.chapterAt(overallTime.value)
      if (chapter != null) {
        coarseSeekTo(chapter.start.seconds + chapter.duration * progress.toDouble(), play = false)
        currentTime.value = chapter.duration * progress.toDouble()
        return
      }
    }
    val positionMs = (progress * player.duration).toLong()
    player.seekTo(positionMs)
    currentTime.value = positionMs.milliseconds
  }

  override fun seekTo(timestamp: Duration) {
    seekTo(timestamp, play = true)
  }

  private fun seekTo(timestamp: Duration, play: Boolean) {
    if (queueShape != QueueShape.CHAPTERS) {
      coarseSeekTo(timestamp, play)
      return
    }
    val timestampInMillis = timestamp.inWholeMilliseconds
    var mediaItemOffsetMs = 0L

    for (index in 0 until player.mediaItemCount) {
      val mediaItem = player.getMediaItemAt(index)
      val mediaItemDuration = mediaItem.mediaMetadata.durationMs ?: error("Media Metadata Corrupted")
      val mediaItemEnd = mediaItemOffsetMs + mediaItemDuration
      if (timestampInMillis in mediaItemOffsetMs until mediaItemEnd) {
        val progressInMediaItem = timestampInMillis - mediaItemOffsetMs
        player.seekTo(index, progressInMediaItem)
        if (play) {
          player.play()
        }
        return
      }
      mediaItemOffsetMs = mediaItemEnd
    }
  }

  override fun skipToNext() {
    if (queueShape != QueueShape.CHAPTERS) {
      // Coarse queues (per-track, single HLS stream) don't transition on chapters; skip by
      // chapter on the absolute timeline instead
      val target = chapterTimeline?.nextChapterStart(overallTime.value)
      if (target != null) {
        coarseSeekTo(target, play = false)
        return
      }
    }
    player.seekToNextMediaItem()
  }

  override fun skipToPrevious() {
    if (queueShape != QueueShape.CHAPTERS) {
      val target = chapterTimeline?.previousChapterTarget(overallTime.value, settings.trackResetThreshold)
      if (target != null) {
        coarseSeekTo(target, play = false)
        return
      }
    }
    if (player.currentPosition.milliseconds > settings.trackResetThreshold) {
      player.seekToDefaultPosition()
      player.play()
    } else {
      player.seekToPreviousMediaItem()
    }
  }

  override fun seekForward() {
    player.seekForward()
  }

  override fun seekBackward() {
    if (player.currentPosition < player.seekBackIncrement && player.currentMediaItemIndex > 0) {
      val previousIndex = player.currentMediaItemIndex - 1
      val previousDurationMs = player.getMediaItemAt(previousIndex)
        .mediaMetadata
        .durationMs
      if (previousDurationMs != null) {
        val seekPositionMs = (previousDurationMs - player.seekBackIncrement).coerceAtLeast(0L)
        player.seekTo(previousIndex, seekPositionMs)
      } else {
        player.seekBack()
      }
    } else {
      player.seekBack()
    }
  }

  override fun setPlaybackSpeed(speed: Float) {
    playbackSpeed.value = speed
    settings.setPlaybackSpeedFor(preparedSession?.libraryItem?.id, speed)
    player.setPlaybackSpeed(speed)
  }

  override fun setTimer(timer: PlaybackTimer) {
    sleepTimerManager.setTimer(timer)
  }

  override fun clearTimer() {
    sleepTimerManager.clearTimer()
  }

  /*
   * Player Listener Callbacks
   */

  override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
    if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_LOCAL) {
      dbark { "onDeviceInfoChanged: Local Player" }
      isRemotePlayback = false
      armCastWatchdog(null)
      updateProgress(player)
    } else if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
      dbark { "onDeviceInfoChanged: Remote Player" }
      if (!isRemotePlayback) {
        isRemotePlayback = true
        // The remote player surfaces no PlaybackException for receiver-side failures
        // (unreachable/unauthorized media, dead receiver) — a handoff that never reaches
        // READY is the only signal, so give it a deadline.
        armCastWatchdog(CAST_HANDOFF_READY_TIMEOUT_MS)
        updateProgress(player)
      }
    }
  }

  /**
   * (Re)arms the dead-cast watchdog with [timeoutMs], or cancels it when null. If the deadline
   * elapses before READY is observed, the cast session is ended and playback falls back to the
   * local player, paused at the last known position.
   */
  private fun armCastWatchdog(timeoutMs: Long?) {
    castWatchdogJob?.cancel()
    castWatchdogJob = if (timeoutMs == null) {
      null
    } else {
      scope.launch {
        delay(timeoutMs)
        onCastPlaybackDead()
      }
    }
  }

  private suspend fun onCastPlaybackDead() {
    wbark { "Cast playback failed to reach READY in time; falling back to local playback" }
    val resumeTime = overallTime.value
    castController.disconnect()

    // The composite CastPlayer swaps back to the local player when the session ends
    withTimeoutOrNull(CAST_FALLBACK_SWAP_TIMEOUT_MS) {
      while (player.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_LOCAL) {
        delay(100)
      }
    }
    // Sync from actual state rather than the listener event, which may not have landed yet —
    // the seek below must use local (chapter-queue) semantics
    isRemotePlayback = player.deviceInfo.playbackType != DeviceInfo.PLAYBACK_TYPE_LOCAL

    player.pause()
    seekTo(resumeTime, play = false)
    GlobalToaster.show(
      context.getString(R.string.cast_failed_resumed_locally),
      Toast.Duration.LONG,
    )
  }

  override fun onPlayerError(error: PlaybackException) {
    ebark { "Playback error: errorCode=${error.errorCode} message=${error.message}" }
    CrashReporter.record(error)

    // An HLS stream that dies (server restarted, ffmpeg reset, playlist gone) is recoverable:
    // the same session can always direct-play its tracks. One attempt only — if the rebuilt
    // direct queue also fails, the error surfaces normally.
    val session = preparedSession
    if (queueShape == QueueShape.SINGLE && !hlsFallbackAttempted && session != null) {
      hlsFallbackAttempted = true
      wbark { "HLS stream failed (errorCode=${error.errorCode}); rebuilding as direct play" }
      val resumeTime = overallTime.value
      val wasPlaying = player.playWhenReady
      val listener = finishedListener ?: { }
      scope.launch {
        prepare(
          session = session.copy(hlsStreamUrl = null, currentTime = resumeTime),
          playImmediately = wasPlaying,
          chapterId = null,
          onFinished = listener,
        )
        GlobalToaster.show(
          context.getString(R.string.hls_failed_switched_to_direct),
          Toast.Duration.LONG,
        )
      }
      return
    }

    _error.value = error
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    if (isRemotePlayback) {
      when (playbackState) {
        // Receiver is healthy; a later stall (powered off, network drop) re-arms below
        Player.STATE_READY -> armCastWatchdog(null)
        // A receiver that vanishes mid-playback leaves the player buffering forever
        // (androidx/media #2182), so bound it
        Player.STATE_BUFFERING -> armCastWatchdog(CAST_STALL_TIMEOUT_MS)
        else -> Unit
      }
    }

    if (playbackState == Player.STATE_ENDED) {
      dbark { "Playback has ended! Mark the item as finished" }
      scope.launch {
        finishedListener?.invoke(preparedSession?.libraryItem?.id ?: return@launch)
      }
    } else if (
      playbackState == Player.STATE_READY &&
      player.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
    ) {
      // Ensure that our remote cast player has up-to-date playback speed set
      // See: https://github.com/androidx/media/issues/889
      player.setPlaybackSpeed(playbackSpeed.value)
    }
  }

  override fun onTimelineChanged(timeline: Timeline, reason: Int) {
    if (queueShape != QueueShape.CHAPTERS) {
      updateProgress(player)
      return
    }
    currentDuration.value = player.duration.milliseconds
  }

  override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
    if (queueShape != QueueShape.CHAPTERS) {
      // Item metadata is stream/track-granular on coarse queues; chapter-relative
      // title/duration are derived from the absolute position in updateProgress instead
      currentMetadata.value = currentMetadata.value.copy(
        artworkUri = mediaMetadata.artworkUri?.toString()
          ?: currentMetadata.value.artworkUri,
      )
      updateProgress(player)
      return
    }
    currentDuration.value = player.duration.milliseconds
    currentMetadata.value = Metadata(
      title = mediaMetadata.title?.toString(),
      artworkUri = mediaMetadata.artworkUri?.toString(),
    )
  }

  override fun onEvents(player: Player, events: Player.Events) {
    eventLogger.vbark {
      buildString {
        appendLine("onEvents {")
        for (i in 0 until events.size()) {
          appendLine("  ${events.get(i).eventAsDebugLog()}")
        }
        appendLine("}")
      }
    }

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

        Player.STATE_ENDED -> AudioPlayer.State.Finished

        else -> if (events.contains(EVENT_DEVICE_INFO_CHANGED)) {
          // This is likely due to changing media routes, i.e. Google Cast, and we should
          // just update the state as buffering and assume that future events will update the
          // state appropriately
          AudioPlayer.State.Buffering
        } else {
          AudioPlayer.State.Disabled
        }
      }

      eventLogger.vbark { "PlaybackState[${player.playbackStateAsDebugLog}] Changed: ${state.value}" }

      if (player.isPlaying) {
        observeProgress(player)
      } else {
        progressJob?.cancel()
      }
    }

    // If the media item transitions (i.e. chapter) and the timer is end of chapter, then
    // stop the playback. Coarse queues (remote per-track, single HLS) transition on tracks
    // or never, so there the boundary detection in updateProgress owns this signal instead.
    if (events.containsAny(EVENT_MEDIA_ITEM_TRANSITION) && queueShape == QueueShape.CHAPTERS) {
      sleepTimerManager.endOfChapter()
    }
  }

  private fun observeProgress(player: Player) {
    progressJob?.cancel()
    progressJob = scope.launch {
      dbark { "Starting Progress Observer" }
      while (isActive) {
        updateProgress(player)
        delay(500L)
      }
    }
    progressJob?.invokeOnCompletion {
      dbark { "Finished Progress Observer" }
    }
  }

  private fun updateProgress(player: Player) {
    val timeline = chapterTimeline
    val shape = queueShape
    if (shape != QueueShape.CHAPTERS && timeline != null) {
      val overall = when (shape) {
        // A single HLS item spans the whole book, so the player position IS absolute
        QueueShape.SINGLE -> player.currentPosition.milliseconds
        else -> timeline.timeAtTrackPosition(player.currentMediaItemIndex, player.currentPosition.milliseconds)
      }
      if (overall != null) {
        detectChapterBoundary(timeline, overall)
        overallTime.value = overall
        updateCoarseChapterState(timeline, overall, player)
        if (shape == QueueShape.SINGLE) {
          // Chapter crossings mid-item produce no player event; the session projection
          // re-derives its virtual chapter window from this tick instead
          sessionPlayer.onChapterProgress()
        }
        return
      }
    }
    currentTime.value = player.currentPosition.milliseconds
    currentDuration.value = player.duration.milliseconds
    overallTime.value = player.overallPosition.milliseconds
  }

  /**
   * Coarse queues have no media-item transition at chapter boundaries, so the end-of-chapter
   * sleep timer is driven by detecting the crossing between progress ticks instead.
   */
  private fun detectChapterBoundary(timeline: ChapterTimeline, overall: Duration) {
    val previous = lastBoundaryCheckTime
    lastBoundaryCheckTime = overall
    if (previous != null && timeline.crossedChapterBoundary(previous, overall)) {
      sleepTimerManager.endOfChapter()
    }
  }

  /**
   * Coarse queues (per-track remote, single-item HLS) don't match the UI's chapter language:
   * re-derive chapter-relative time, duration, and title from the absolute position so the
   * playback UI matches chapter-granular queues exactly.
   */
  private fun updateCoarseChapterState(timeline: ChapterTimeline, overall: Duration, player: Player) {
    val progress = timeline.progressAt(overall)
    if (progress != null) {
      currentTime.value = progress.position
      currentDuration.value = progress.duration
      if (currentMetadata.value.title != progress.chapter.title) {
        currentMetadata.value = currentMetadata.value.copy(title = progress.chapter.title)
      }
    } else {
      currentTime.value = player.currentPosition.milliseconds
      currentDuration.value = player.duration.milliseconds
    }
  }

  /**
   * Seeks on a coarse queue by translating the absolute timestamp through the session's
   * track table (queue items may not carry our duration metadata across the cast boundary).
   */
  private fun coarseSeekTo(timestamp: Duration, play: Boolean) {
    if (queueShape == QueueShape.SINGLE) {
      // One stream item: the absolute timestamp needs no track translation
      player.seekTo(0, timestamp.inWholeMilliseconds)
    } else {
      val timeline = chapterTimeline ?: return
      val position = timeline.trackPositionAt(timestamp) ?: return
      player.seekTo(position.queueIndex, position.offset.inWholeMilliseconds)
    }
    overallTime.value = timestamp
    // A deliberate seek is not a chapter boundary crossing
    lastBoundaryCheckTime = timestamp
    if (play) {
      player.play()
    }
  }
}

/**
 * Helper property to compute the overall time in a library item that the playback is currently at.
 * Since we divvy chapters into individual mediaItems, the [Player.getCurrentPosition] returns the postion
 * relative to the current media item
 */
internal val Player.overallPosition: Long get() {
  var timelineOffsetMs = 0L
  val currentIndex = currentMediaItemIndex
  (0 until currentIndex).forEach { index ->
    timelineOffsetMs += getMediaItemAt(index)
      .mediaMetadata
      .durationMs
      ?: 0L
  }

  return timelineOffsetMs + currentPosition
}
