// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.OnFinishedListener
import app.campfire.audioplayer.PlaybackEngineUnavailableException
import app.campfire.audioplayer.impl.chapters.ChapterTimeline
import app.campfire.audioplayer.impl.engine.EngineState
import app.campfire.audioplayer.impl.engine.PlaybackEngine
import app.campfire.audioplayer.impl.engine.PlaybackEngineEvent
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import app.campfire.audioplayer.impl.mediaitem.MediaItemBuilder
import app.campfire.audioplayer.impl.sleep.SleepTimerManager
import app.campfire.audioplayer.impl.sleep.VolumeFadeController
import app.campfire.audioplayer.model.EqualizerState
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.audioplayer.model.profileOrNull
import app.campfire.core.audio.EqualizerBands
import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.extensions.seconds
import app.campfire.core.image.CoverUrls
import app.campfire.core.logging.Cork
import app.campfire.core.model.Session
import app.campfire.core.model.UserId
import app.campfire.core.model.loggableId
import app.campfire.crashreporting.CrashReporter
import app.campfire.settings.api.EqualizerSettings
import app.campfire.settings.api.PlaybackSettings
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The desktop [AudioPlayer]: a queue of whole audio files driven through a single-item
 * [PlaybackEngine], with chapter semantics derived from the session's [ChapterTimeline].
 *
 * Queue shape: one item per audio track (or one item for a podcast episode / HLS stream). Books
 * are never sliced into clipped chapter items — a single-file book is opened once and chapters
 * are virtual boundaries on the absolute timeline, exactly like the Cast and HLS paths on Android.
 * Every seek is expressed as an absolute session time, translated to (track, offset) here.
 *
 * Authentication: track URLs are hydrated without credentials; [AccessTokenProvider] supplies the
 * token that is appended as a query parameter when an item is opened (the only mechanism libvlc
 * supports). ABS access tokens expire after about an hour, so a very long single track may need
 * a re-open after expiry — the play-session flow with credential-free URLs is the planned fix.
 *
 * Threading: all mutable state below is confined to [engineDispatcher], a single thread. Public
 * [AudioPlayer] calls (which arrive on the UI thread) enqueue work onto it and return immediately,
 * so native calls never block the UI. Engine events are collected on the same thread. The exposed
 * [StateFlow]s are the only cross-thread surface.
 */
class DesktopAudioPlayer(
  private val settings: PlaybackSettings,
  private val equalizerSettings: EqualizerSettings,
  sleepTimerManagerFactory: SleepTimerManager.Factory,
  private val engineFactory: PlaybackEngine.Factory,
  private val accessTokenProvider: AccessTokenProvider,
  private val engineDispatcher: CoroutineDispatcher = newEngineDispatcher(),
) : AudioPlayer {

  /**
   * Supplies the current ABS access token for a user. libvlc can't attach request headers, so
   * the token rides in the media URL as `?token=`; it is re-read on every item open so a token
   * refreshed mid-session is picked up at the next track or cross-track seek.
   */
  fun interface AccessTokenProvider {
    suspend fun accessToken(userId: UserId): String?
  }

  private val scope = CoroutineScope(SupervisorJob() + engineDispatcher)

  private val sleepTimerManager = sleepTimerManagerFactory.create(this)

  @Volatile
  override var preparedSession: Session? = null

  override val state = MutableStateFlow(AudioPlayer.State.Disabled)
  override val error = MutableStateFlow<Throwable?>(null)
  override val overallTime = MutableStateFlow(Duration.ZERO)
  override val currentTime = MutableStateFlow(Duration.ZERO)
  override val currentDuration = MutableStateFlow(Duration.ZERO)
  override val currentMetadata = MutableStateFlow(Metadata())
  override val playbackSpeed = MutableStateFlow(settings.playbackSpeed)
  override val equalizer = MutableStateFlow<EqualizerState>(
    EqualizerState.Available(equalizerSettings.equalizerProfile),
  )

  override val runningTimer: StateFlow<RunningTimer?>
    get() = sleepTimerManager.runningTimer

  // region Engine-thread-confined state (the @Volatile fields are also read from callers)

  @Volatile
  private var engine: PlaybackEngine? = null
  private var eventsJob: Job? = null
  private var finishedListener: OnFinishedListener? = null

  private var timeline: ChapterTimeline? = null
  private var queue: List<MediaItem> = emptyList()
  private var currentIndex = 0

  /** True when the single queue item spans the whole session (HLS), so item position is absolute. */
  private var singleStream = false

  /** Engine-reported duration of the current item, once probed. */
  private var itemDuration: Duration? = null

  /** The user's intent — distinct from the engine's transient state (opening, buffering). */
  private var playWhenReady = false
  private var engineState = EngineState.Idle

  @Volatile
  private var stopped = false
  private var lastBoundaryCheckTime: Duration? = null

  private var fadeJob: Job? = null
  private var stallJob: Job? = null

  @Volatile
  private var previousVolumeLevel = 0f

  // endregion

  override suspend fun prepare(
    session: Session,
    playImmediately: Boolean,
    chapterId: Int?,
    onFinished: OnFinishedListener,
  ) = withContext(engineDispatcher) {
    if (stopped) {
      wbark { "prepare() after stop(); ignoring ${session.libraryItem.id.loggableId}" }
      return@withContext
    }
    preparedSession = session
    finishedListener = onFinished
    error.value = null
    playbackSpeed.value = settings.playbackSpeedFor(session.libraryItem.id)
    equalizer.value = EqualizerState.Available(equalizerSettings.equalizerProfileFor(session.libraryItem.id))
    state.value = AudioPlayer.State.Initializing

    val engine = ensureEngine() ?: return@withContext

    val timeline = ChapterTimeline(session).also { this@DesktopAudioPlayer.timeline = it }
    singleStream = session.episode == null && session.hlsStreamUrl != null
    queue = if (singleStream) MediaItemBuilder.build(session) else MediaItemBuilder.buildTracks(session)
    itemDuration = null
    lastBoundaryCheckTime = null

    if (queue.isEmpty()) {
      fail(InvalidPlaybackSessionException(session, "Session produced no playable media items"))
      return@withContext
    }

    ibark {
      "Prepare Session(itemId=${session.libraryItem.id.loggableId}, playMethod=${session.playMethod}, " +
        "currentTime=${session.currentTime}, chapterId=$chapterId, items=${queue.size}, singleStream=$singleStream)"
    }

    val resume = session.currentTime.takeIf { it.isFinite() && it > Duration.ZERO } ?: Duration.ZERO
    val start = chapterId?.let { id ->
      timeline.startOfLocalQueueIndex(id) ?: run {
        wbark { "Chapter/track $id not found on ${session.libraryItem.id.loggableId}; resuming at $resume" }
        null
      }
    } ?: resume

    val (index, offset) = locate(timeline, start)
    currentIndex = index
    overallTime.value = start
    lastBoundaryCheckTime = start
    publishChapterState(start, offset)

    if (playImmediately) {
      sleepTimerManager.onSessionStart()
    }
    playWhenReady = playImmediately
    openItem(engine, index, offset, playImmediately)
  }

  override fun release() {
    stop()
  }

  override fun pause() = onEngine {
    playWhenReady = false
    engine?.pause()
  }

  override fun fadeToPause(duration: Duration, tickRate: Long): Job {
    previousVolumeLevel = engine?.volume ?: 1f
    fadeJob?.cancel()
    return VolumeFadeController.fade(
      scope = scope,
      duration = duration,
      tickRate = tickRate,
      getVolume = { engine?.volume ?: 0f },
      setVolume = { engine?.volume = it },
      onPause = {
        playWhenReady = false
        engine?.pause()
      },
    ).also { fadeJob = it }
  }

  override fun playPause() = onEngine {
    val engine = engine ?: return@onEngine
    if (state.value == AudioPlayer.State.Paused) {
      sleepTimerManager.onSessionStart()
    }

    // Restore the volume a fade left at zero
    if (engine.volume == 0f) {
      engine.volume = if (previousVolumeLevel > 0f) previousVolumeLevel else 1f
    }

    if (playWhenReady) {
      playWhenReady = false
      engine.pause()
    } else {
      playWhenReady = true
      engine.play()
    }
  }

  override fun stop() {
    stopped = true
    preparedSession = null
    finishedListener = null
    scope.launch {
      fadeJob?.cancel()
      eventsJob?.cancel()
      engine?.let { engine ->
        engine.stop()
        engine.release()
      }
      engine = null
      state.value = AudioPlayer.State.Disabled
    }.invokeOnCompletion {
      scope.cancel()
      (engineDispatcher as? ExecutorCoroutineDispatcher)?.close()
    }
  }

  override fun seekTo(itemIndex: Int) = onEngine {
    val target = timeline?.startOfLocalQueueIndex(itemIndex) ?: return@onEngine
    seekAbsolute(target, play = true)
  }

  override fun seekTo(progress: Float) = onEngine {
    val timeline = timeline ?: return@onEngine
    val fraction = progress.coerceIn(0f, 1f).toDouble()
    val chapter = timeline.chapterAt(overallTime.value)
    val target = if (chapter != null) {
      chapter.start.seconds + chapter.duration * fraction
    } else {
      currentItemStart() + currentItemDuration() * fraction
    }
    seekAbsolute(target, play = false)
  }

  override fun seekTo(timestamp: Duration) = onEngine {
    seekAbsolute(timestamp, play = true)
  }

  override fun skipToNext() = onEngine {
    val timeline = timeline ?: return@onEngine
    val target = timeline.nextChapterStart(overallTime.value)
      ?: nextTrackStart(timeline)
      ?: return@onEngine
    seekAbsolute(target, play = false)
  }

  override fun skipToPrevious() = onEngine {
    val timeline = timeline ?: return@onEngine
    val threshold = settings.trackResetThreshold
    val target = timeline.previousChapterTarget(overallTime.value, threshold)
      ?: previousTrackTarget(timeline, threshold)
    seekAbsolute(target, play = false)
  }

  override fun seekForward() = onEngine {
    seekAbsolute(overallTime.value + settings.forwardTimeMs.milliseconds, play = false)
  }

  override fun seekBackward() = onEngine {
    seekAbsolute(overallTime.value - settings.backwardTimeMs.milliseconds, play = false)
  }

  override fun setPlaybackSpeed(speed: Float) {
    playbackSpeed.value = speed
    onEngine {
      settings.setPlaybackSpeedFor(preparedSession?.libraryItem?.id, speed)
      engine?.setRate(speed)
    }
  }

  override fun setEqualizer(profile: EqualizerProfile) {
    equalizer.value = EqualizerState.Available(profile)
    onEngine {
      equalizerSettings.setEqualizerProfileFor(preparedSession?.libraryItem?.id, profile)
      engine?.apply(profile)
    }
  }

  override fun setTimer(timer: PlaybackTimer) {
    sleepTimerManager.setTimer(timer)
  }

  override fun clearTimer() {
    sleepTimerManager.clearTimer()
  }

  // region Engine thread

  private fun onEngine(block: suspend () -> Unit) {
    scope.launch { block() }
  }

  /** Opens [index] at [offset], stamping the freshest access token onto the item's URL. */
  private suspend fun openItem(engine: PlaybackEngine, index: Int, offset: Duration, playWhenReady: Boolean) {
    val item = queue[index]
    val userId = preparedSession?.userId
    val token = userId?.let { accessTokenProvider.accessToken(it) }
    engine.open(item.copy(uri = item.uri.withAccessToken(token)), offset, playWhenReady)
  }

  /**
   * Creates the engine on first use — native discovery and library loading happen here, on the
   * engine thread, never on the UI thread. A platform without a usable engine surfaces through
   * [error] as [PlaybackEngineUnavailableException].
   */
  private fun ensureEngine(): PlaybackEngine? {
    engine?.let { return it }
    return try {
      engineFactory.create().also { created ->
        engine = created
        created.setRate(playbackSpeed.value)
        created.apply(equalizer.value.profileOrNull ?: equalizerSettings.equalizerProfile)
        eventsJob = scope.launch {
          created.events.collect { onEngineEvent(it) }
        }
      }
    } catch (e: PlaybackEngineUnavailableException) {
      ebark(e) { "No playback engine available" }
      error.value = e
      state.value = AudioPlayer.State.Disabled
      null
    }
  }

  private suspend fun onEngineEvent(event: PlaybackEngineEvent) {
    when (event) {
      is PlaybackEngineEvent.StateChanged -> onEngineStateChanged(event.state)
      is PlaybackEngineEvent.PositionChanged -> onPositionChanged(event.position)
      is PlaybackEngineEvent.DurationChanged -> {
        itemDuration = event.duration
        if (timeline?.chapterAt(overallTime.value) == null) {
          currentDuration.value = event.duration
        }
      }
      is PlaybackEngineEvent.Error -> fail(event.cause)
    }
  }

  private suspend fun onEngineStateChanged(newState: EngineState) {
    if (newState == EngineState.Buffering) {
      // libvlc reports cache fill continuously: while paused (prefetching after a seek) it
      // means nothing, and while playing it is only a stall if the position stops advancing
      when (engineState) {
        EngineState.Paused -> return
        EngineState.Playing -> {
          scheduleStall()
          return
        }
        else -> Unit
      }
    }
    stallJob?.cancel()
    if (newState != engineState) dbark { "engine state: $engineState -> $newState (playWhenReady=$playWhenReady)" }
    engineState = newState
    when (newState) {
      EngineState.Opening,
      EngineState.Buffering,
      -> state.value = AudioPlayer.State.Buffering

      EngineState.Playing -> state.value = AudioPlayer.State.Playing
      EngineState.Paused -> state.value = AudioPlayer.State.Paused

      // A stop we didn't ask for is the engine unloading the previous item mid-transition
      EngineState.Idle -> if (stopped) state.value = AudioPlayer.State.Disabled

      EngineState.Ended -> onItemEnded()
    }
  }

  /** Flags a stall only if no position tick arrives within [STALL_GRACE]. */
  private fun scheduleStall() {
    if (stallJob?.isActive == true) return
    stallJob = scope.launch {
      delay(STALL_GRACE)
      if (engineState == EngineState.Playing) {
        dbark { "engine stalled" }
        engineState = EngineState.Buffering
        state.value = AudioPlayer.State.Buffering
      }
    }
  }

  private fun onPositionChanged(position: Duration) {
    stallJob?.cancel()
    // Position advancing while we believe we're stalled means the stall cleared
    if (engineState == EngineState.Buffering && playWhenReady) {
      engineState = EngineState.Playing
      state.value = AudioPlayer.State.Playing
    }
    val absolute = absoluteTimeOf(position)
    detectChapterBoundary(absolute)
    overallTime.value = absolute
    publishChapterState(absolute, position)
  }

  private suspend fun onItemEnded() {
    val timeline = timeline
    val engine = engine
    if (timeline == null || engine == null || singleStream || currentIndex >= queue.lastIndex) {
      finish()
      return
    }

    // Chaptered books get end-of-chapter from boundary detection on the next position tick;
    // for chapterless books the track boundary is the chapter boundary.
    val timerTriggered = !timeline.hasChapters && sleepTimerManager.endOfChapter()
    if (timerTriggered) playWhenReady = false

    currentIndex += 1
    itemDuration = null
    val start = timeline.timeAtTrackPosition(currentIndex, Duration.ZERO) ?: overallTime.value
    overallTime.value = start
    lastBoundaryCheckTime = start
    publishChapterState(start, Duration.ZERO)
    state.value = AudioPlayer.State.Buffering
    openItem(engine, currentIndex, Duration.ZERO, playWhenReady)
  }

  private fun finish() {
    val session = preparedSession
    state.value = AudioPlayer.State.Finished
    ibark { "finished(${session?.libraryItem?.id?.loggableId})" }
    val listener = finishedListener ?: return
    val itemId = session?.libraryItem?.id ?: return
    scope.launch { listener(itemId) }
  }

  private fun fail(cause: Throwable) {
    ebark(cause) { "Playback error" }
    CrashReporter.record(cause)
    error.value = cause
    // Leave the controls usable rather than stuck on a spinner
    if (state.value == AudioPlayer.State.Buffering || state.value == AudioPlayer.State.Initializing) {
      state.value = AudioPlayer.State.Paused
    }
  }

  /**
   * Seek the absolute session timeline, opening a different track when the target lives in one.
   * [play] forces playback to start; otherwise the current intent is preserved across the seek.
   */
  private suspend fun seekAbsolute(target: Duration, play: Boolean) {
    val timeline = timeline ?: return
    val engine = engine ?: return
    val session = preparedSession ?: return
    val clamped = target.coerceIn(Duration.ZERO, session.duration)

    val (index, offset) = locate(timeline, clamped)
    if (index == currentIndex) {
      engine.seekTo(offset)
      if (play && !playWhenReady) {
        playWhenReady = true
        engine.play()
      }
    } else {
      currentIndex = index
      itemDuration = null
      playWhenReady = play || playWhenReady
      state.value = AudioPlayer.State.Buffering
      openItem(engine, index, offset, playWhenReady)
    }

    overallTime.value = clamped
    // A deliberate seek is not a chapter boundary crossing
    lastBoundaryCheckTime = clamped
    publishChapterState(clamped, offset)
  }

  /** (queue index, offset within item) for an absolute time. */
  private fun locate(timeline: ChapterTimeline, absolute: Duration): Pair<Int, Duration> {
    if (singleStream) return 0 to absolute
    val position = timeline.trackPositionAt(absolute) ?: return 0 to Duration.ZERO
    return position.queueIndex.coerceIn(queue.indices) to position.offset
  }

  private fun absoluteTimeOf(positionInItem: Duration): Duration {
    if (singleStream) return positionInItem
    return timeline?.timeAtTrackPosition(currentIndex, positionInItem) ?: positionInItem
  }

  private fun currentItemStart(): Duration = absoluteTimeOf(Duration.ZERO)

  private fun currentItemDuration(): Duration {
    return itemDuration
      ?: queue.getOrNull(currentIndex)?.metadata?.durationMs?.milliseconds
      ?: preparedSession?.duration
      ?: Duration.ZERO
  }

  private fun nextTrackStart(timeline: ChapterTimeline): Duration? {
    if (timeline.hasChapters || singleStream || currentIndex >= queue.lastIndex) return null
    return timeline.timeAtTrackPosition(currentIndex + 1, Duration.ZERO)
  }

  private fun previousTrackTarget(timeline: ChapterTimeline, threshold: Duration): Duration {
    val trackStart = currentItemStart()
    val progressInTrack = overallTime.value - trackStart
    return if (progressInTrack > threshold || currentIndex == 0 || singleStream) {
      trackStart
    } else {
      timeline.timeAtTrackPosition(currentIndex - 1, Duration.ZERO) ?: trackStart
    }
  }

  /**
   * Coarse queues have no item transition at chapter boundaries, so the end-of-chapter sleep
   * timer is driven by detecting the crossing between position ticks instead.
   */
  private fun detectChapterBoundary(absolute: Duration) {
    val timeline = timeline ?: return
    val previous = lastBoundaryCheckTime
    lastBoundaryCheckTime = absolute
    if (previous != null && timeline.crossedChapterBoundary(previous, absolute)) {
      sleepTimerManager.endOfChapter()
    }
  }

  /**
   * Re-derive the chapter-relative time, duration, and title the UI renders from the absolute
   * position; chapterless items fall back to track-relative values.
   */
  private fun publishChapterState(absolute: Duration, positionInItem: Duration) {
    val session = preparedSession ?: return
    val artwork = CoverUrls.sized(session.libraryItem.media.coverImageUrl, CoverUrls.ARTWORK_WIDTH)
    val progress = timeline?.progressAt(absolute)
    if (progress != null) {
      currentTime.value = progress.position
      currentDuration.value = progress.duration
      currentMetadata.value = Metadata(title = progress.chapter.title, artworkUri = artwork)
    } else {
      val item = queue.getOrNull(currentIndex)
      currentTime.value = positionInItem
      currentDuration.value = currentItemDuration()
      currentMetadata.value = Metadata(title = item?.metadata?.title, artworkUri = artwork)
    }
  }

  /** Appends `token=` to an http(s) URL that doesn't already carry one. */
  private fun String.withAccessToken(token: String?): String {
    if (token.isNullOrEmpty() || !startsWith("http", ignoreCase = true) || contains("token=")) return this
    val separator = if (contains('?')) '&' else '?'
    return "$this${separator}token=$token"
  }

  private fun PlaybackEngine.apply(profile: EqualizerProfile) {
    // libvlc has no dedicated loudness/bass effects: the loudness slider maps onto the
    // equalizer preamp, and the bass slider folds extra gain onto the two lowest bands
    // (60/170 Hz). The engine re-clamps everything to its own limits.
    val bassBoostDb = profile.bassBoost.coerceIn(EqualizerBands.BassBoostRange) * BASS_BOOST_MAX_DB
    val bandGainsDb = profile.bandGainsDb.mapIndexed { index, gainDb ->
      val gain = gainDb.coerceIn(EqualizerBands.BandGainRangeDb)
      if (index < BASS_BOOST_BAND_COUNT) gain + bassBoostDb else gain
    }
    setEqualizer(
      enabled = profile.enabled,
      preampDb = profile.loudnessGainDb.coerceIn(EqualizerBands.LoudnessGainRangeDb),
      bandGainsDb = bandGainsDb,
    )
  }

  // endregion

  companion object : Cork {
    override val tag: String = "DesktopAudioPlayer"
    override val enabled: Boolean = true

    private const val BASS_BOOST_MAX_DB = 6f
    private val STALL_GRACE = 750.milliseconds
    private const val BASS_BOOST_BAND_COUNT = 2

    private fun newEngineDispatcher(): CoroutineDispatcher {
      return Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "campfire-audio-engine").apply { isDaemon = true }
      }.asCoroutineDispatcher()
    }
  }
}
