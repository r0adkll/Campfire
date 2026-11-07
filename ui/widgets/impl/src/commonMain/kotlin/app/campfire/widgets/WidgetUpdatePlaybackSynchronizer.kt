// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.core.di.AppScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the player widget in step with playback.
 *
 * Every widget update re-renders and re-sends the whole widget on the app's main thread, so the
 * playing position is only pushed when it would change what the widget shows: its remaining-time
 * readout or its progress bar, and then no more than once every [TIME_UPDATE_INTERVAL_MS].
 */
@ContributesIntoSet(AppScope::class)
@Inject
class WidgetUpdatePlaybackSynchronizer(
  private val widgetUpdater: WidgetUpdater,
  private val widgetPinRequester: WidgetPinRequester,
  private val fatherTime: FatherTime,
) : PlaybackSynchronizer {

  // Callbacks for the different player flows arrive concurrently; this also keeps widget updates
  // from overlapping one another
  private val lock = Mutex()

  // Null until the player reports a position, so the widget's saved one isn't overwritten with zero
  private var currentTime: Duration? = null
  private var currentDuration = Duration.ZERO
  private var playbackSpeed = 1f

  private var shownReadout: WidgetTimeReadout? = null
  private var lastTimeUpdate = 0L

  override suspend fun onStateChanged(
    sessionId: Uuid,
    libraryItemId: LibraryItemId,
    state: AudioPlayer.State,
    previousState: AudioPlayer.State,
  ) {
    // With the position too: pausing can land between time updates, and the widget would then keep
    // showing the last one sent
    lock.withLock { publish(currentTime = currentTime) }

    // Prompt the user to pin the playback widget if they haven't seen it yet
    if (previousState != AudioPlayer.State.Playing && state == AudioPlayer.State.Playing) {
      widgetPinRequester.requestPinWidget()
    }
  }

  override suspend fun onMetadataChanged(
    libraryItemId: LibraryItemId,
    metadata: Metadata,
  ) {
    lock.withLock { widgetUpdater.updatePlayerWidget() }
  }

  override suspend fun onCurrentTimeChanged(
    libraryItemId: LibraryItemId,
    currentTime: Duration,
  ) = lock.withLock {
    this.currentTime = currentTime
    val readout = readout()
    val now = fatherTime.nowInEpochMillis()
    if (readout != shownReadout && now - lastTimeUpdate >= TIME_UPDATE_INTERVAL_MS) {
      publish(currentTime = currentTime)
    }
  }

  override suspend fun onCurrentDurationChanged(
    libraryItemId: LibraryItemId,
    currentDuration: Duration,
  ) = lock.withLock {
    this.currentDuration = currentDuration.takeIf { it.isFinite() } ?: Duration.ZERO
    // A new chapter: send the position with it, so the widget doesn't pair the new duration with
    // the last position it was given from the previous chapter
    publish(currentTime = currentTime, currentDuration = this.currentDuration)
  }

  override suspend fun onPlaybackSpeedChanged(
    libraryItemId: LibraryItemId,
    playbackSpeed: Float,
  ) = lock.withLock {
    this.playbackSpeed = playbackSpeed
    publish(currentTime = currentTime, playbackSpeed = playbackSpeed)
  }

  private suspend fun publish(
    currentTime: Duration?,
    currentDuration: Duration? = null,
    playbackSpeed: Float? = null,
  ) {
    widgetUpdater.updatePlayerWidget(currentTime, currentDuration, playbackSpeed)
    if (currentTime != null) {
      shownReadout = readout()
      lastTimeUpdate = fatherTime.nowInEpochMillis()
    }
  }

  private fun readout(): WidgetTimeReadout =
    WidgetTimeReadout.of(currentTime ?: Duration.ZERO, currentDuration, playbackSpeed)

  companion object {
    internal const val TIME_UPDATE_INTERVAL_MS = 5000L
  }
}

/** What the widget shows of the playing position. */
internal data class WidgetTimeReadout(
  val remaining: String,
  val progressPercent: Int,
) {
  companion object {
    fun of(currentTime: Duration, currentDuration: Duration, playbackSpeed: Float): WidgetTimeReadout {
      if (currentDuration <= Duration.ZERO) return WidgetTimeReadout(remaining = "", progressPercent = 0)
      return WidgetTimeReadout(
        // As the widget formats it
        remaining = (currentDuration - currentTime).div(playbackSpeed.toDouble()).readoutFormat(largestOnly = true),
        progressPercent = (currentTime / currentDuration * 100).roundToInt(),
      )
    }
  }
}
