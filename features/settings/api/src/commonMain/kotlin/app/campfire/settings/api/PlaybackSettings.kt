// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.model.LibraryItemId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.StateFlow

interface PlaybackSettings {

  val enableMp3IndexSeeking: Boolean
  fun setEnableMp3IndexSeeking(value: Boolean)
  fun observeMp3IndexSeeking(): StateFlow<Boolean>

  val forwardTimeMs: Long
  fun setForwardTimeMs(value: Long)
  val backwardTimeMs: Long
  fun setBackwardTimeMs(value: Long)
  fun observeForwardTimeMs(): StateFlow<Long>
  fun observeBackwardTimeMs(): StateFlow<Long>

  val trackResetThreshold: Duration
  fun setTrackResetThreshold(value: Duration)
  fun observeTrackResetThreshold(): StateFlow<Duration>

  val playbackRates: List<Float>
  fun setPlaybackRates(value: List<Float>)
  fun observePlaybackRates(): StateFlow<List<Float>>

  val playbackSpeed: Float
  fun observePlaybackSpeed(): StateFlow<Float>
  fun setPlaybackSpeed(value: Float)

  /**
   * Per-item playback speed overrides, keyed by library item id. The presence of an entry means the
   * item has a per-item speed enabled, and its value is that item's saved speed. Items without an
   * entry use the global [playbackSpeed].
   */
  val itemPlaybackSpeeds: Map<LibraryItemId, Float>
  fun setItemPlaybackSpeeds(value: Map<LibraryItemId, Float>)
  fun observeItemPlaybackSpeeds(): StateFlow<Map<LibraryItemId, Float>>

  /**
   * The effective playback speed for [itemId] — its per-item override if one is enabled,
   * otherwise the global [playbackSpeed].
   */
  fun playbackSpeedFor(itemId: LibraryItemId?): Float {
    return itemId?.let { itemPlaybackSpeeds[it] } ?: playbackSpeed
  }

  /**
   * Persist [speed] to [itemId]'s per-item override when one is enabled, otherwise to the
   * global [playbackSpeed].
   */
  fun setPlaybackSpeedFor(itemId: LibraryItemId?, speed: Float) {
    if (itemId != null && itemId in itemPlaybackSpeeds) {
      setItemPlaybackSpeeds(itemPlaybackSpeeds + (itemId to speed))
    } else {
      setPlaybackSpeed(speed)
    }
  }

  /**
   * When true, remote control next/previous buttons skip to next/previous chapter.
   * When false, they seek forward/backward by the configured time.
   */
  val remoteNextPrevSkipsChapters: Boolean
  fun setRemoteNextPrevSkipsChapters(value: Boolean)
  fun observeRemoteNextPrevSkipsChapters(): StateFlow<Boolean>

  /**
   * When true, we will show sync opportunities to the user (or allow auto-sync if enabled)
   */
  val syncEnabled: Boolean
  fun setSyncEnabled(value: Boolean)
  fun observeSyncEnabled(): StateFlow<Boolean>

  /**
   * When true, a new session will use the media progress if it is newer than the previous session when resuming playback.
   * When false, it will continue to use the local session progress
   */
  val autoSyncEnabled: Boolean
  fun setAutoSyncEnabled(value: Boolean)
  fun observeAutoSyncEnabled(): StateFlow<Boolean>

  /**
   * When true, playback actions (play, pause, seek, etc.) will be recorded to a local history.
   * When false, playback history is disabled and all existing history is cleared.
   */
  val playbackHistoryEnabled: Boolean
  fun setPlaybackHistoryEnabled(value: Boolean)
  fun observePlaybackHistoryEnabled(): StateFlow<Boolean>

  /**
   * The minimum interval between listening syncs to the server while on an unmetered
   * connection (Wi-Fi). Constrained to [SyncIntervalRange].
   */
  val syncIntervalUnmetered: Duration
  fun setSyncIntervalUnmetered(value: Duration)
  fun observeSyncIntervalUnmetered(): StateFlow<Duration>

  /**
   * The minimum interval between listening syncs to the server while on a metered
   * connection (mobile data). Constrained to [SyncIntervalRange].
   */
  val syncIntervalMetered: Duration
  fun setSyncIntervalMetered(value: Duration)
  fun observeSyncIntervalMetered(): StateFlow<Duration>

  /**
   * How streamed items are delivered — see [StreamingMethod]. Defaults to
   * [StreamingMethod.DIRECT_PLAY_ONLY] for now; intended to default to [StreamingMethod.AUTO]
   * once the HLS route has proven itself in the wild.
   */
  val streamingMethod: StreamingMethod
  fun setStreamingMethod(value: StreamingMethod)
  fun observeStreamingMethod(): StateFlow<StreamingMethod>

  /**
   * When true, resuming playback after a pause rewinds by an amount that scales with how long playback was
   * paused, per the sliding window derived from [resumeRewindConfig].
   */
  val autoRewindOnResumeEnabled: Boolean
  fun setAutoRewindOnResumeEnabled(value: Boolean)
  fun observeAutoRewindOnResumeEnabled(): StateFlow<Boolean>

  /**
   * The configuration (min pause floor + rewind range) from which the auto-rewind sliding window is derived.
   * See [ResumeRewindConfig], [ResumeRewindConfig.tiers], and [rewindForPause].
   */
  val resumeRewindConfig: ResumeRewindConfig
  fun setResumeRewindConfig(value: ResumeRewindConfig)
  fun observeResumeRewindConfig(): StateFlow<ResumeRewindConfig>

  /**
   * When true, an auto-rewind on resume never crosses back past the start of the current chapter — if the
   * rewind would go before the chapter boundary, it stops at the boundary instead.
   */
  val autoRewindStopAtChapterBoundary: Boolean
  fun setAutoRewindStopAtChapterBoundary(value: Boolean)
  fun observeAutoRewindStopAtChapterBoundary(): StateFlow<Boolean>

  /**
   * Transient, persisted marker for a pause that may still owe a rewind on resume. Persisted so that a pause
   * interrupted by the app being killed still rewinds when playback resumes. Null when no pause is pending.
   */
  val pendingResumeRewind: PendingResumeRewind?
  fun observePendingResumeRewind(): StateFlow<PendingResumeRewind?>
  fun setPendingResumeRewind(value: PendingResumeRewind?)

  /**
   * When true, the book's overall time will display in a progress bar in
   * in the playback ui.
   */
  val bookTimeInPlaybackUi: Boolean
  fun setBookTimeInPlaybackUi(value: Boolean)
  fun observeBookTimeInPlaybackUi(): StateFlow<Boolean>

  /**
   * When true, the playback slider will be wavy
   */
  val playbackWavyScrubber: Boolean
  fun setPlaybackWavyScrubber(value: Boolean)
  fun observePlaybackWavyScrubber(): StateFlow<Boolean>

  /**
   * When true, chapter and book titles too long for the player scroll a few times; otherwise they
   * are ellipsized.
   */
  val scrollingTitles: Boolean
  fun setScrollingTitles(value: Boolean)
  fun observeScrollingTitles(): StateFlow<Boolean>
}

/** The configurable bounds for the metered/unmetered listening-sync intervals. */
val SyncIntervalRange: ClosedRange<Duration> = 5.seconds..5.minutes
