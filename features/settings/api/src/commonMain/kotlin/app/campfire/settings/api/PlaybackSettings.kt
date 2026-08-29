// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.model.LibraryItemId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.StateFlow

interface PlaybackSettings {

  var enableMp3IndexSeeking: Boolean
  fun observeMp3IndexSeeking(): StateFlow<Boolean>

  var forwardTimeMs: Long
  var backwardTimeMs: Long
  fun observeForwardTimeMs(): StateFlow<Long>
  fun observeBackwardTimeMs(): StateFlow<Long>

  var trackResetThreshold: Duration
  fun observeTrackResetThreshold(): StateFlow<Duration>

  var playbackRates: List<Float>
  fun observePlaybackRates(): StateFlow<List<Float>>

  var playbackSpeed: Float

  /**
   * Per-item playback speed overrides, keyed by library item id. The presence of an entry means the
   * item has a per-item speed enabled, and its value is that item's saved speed. Items without an
   * entry use the global [playbackSpeed].
   */
  var itemPlaybackSpeeds: Map<LibraryItemId, Float>
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
      itemPlaybackSpeeds = itemPlaybackSpeeds + (itemId to speed)
    } else {
      playbackSpeed = speed
    }
  }

  /**
   * When true, remote control next/previous buttons skip to next/previous chapter.
   * When false, they seek forward/backward by the configured time.
   */
  var remoteNextPrevSkipsChapters: Boolean
  fun observeRemoteNextPrevSkipsChapters(): StateFlow<Boolean>

  /**
   * When true, we will show sync opportunities to the user (or allow auto-sync if enabled)
   */
  var syncEnabled: Boolean
  fun observeSyncEnabled(): StateFlow<Boolean>

  /**
   * When true, a new session will use the media progress if it is newer than the previous session when resuming playback.
   * When false, it will continue to use the local session progress
   */
  var autoSyncEnabled: Boolean
  fun observeAutoSyncEnabled(): StateFlow<Boolean>

  /**
   * When true, playback actions (play, pause, seek, etc.) will be recorded to a local history.
   * When false, playback history is disabled and all existing history is cleared.
   */
  var playbackHistoryEnabled: Boolean
  fun observePlaybackHistoryEnabled(): StateFlow<Boolean>

  /**
   * The minimum interval between listening syncs to the server while on an unmetered
   * connection (Wi-Fi). Constrained to [SyncIntervalRange].
   */
  var syncIntervalUnmetered: Duration
  fun observeSyncIntervalUnmetered(): StateFlow<Duration>

  /**
   * The minimum interval between listening syncs to the server while on a metered
   * connection (mobile data). Constrained to [SyncIntervalRange].
   */
  var syncIntervalMetered: Duration
  fun observeSyncIntervalMetered(): StateFlow<Duration>

  /**
   * How streamed items are delivered — see [StreamingMethod]. Defaults to
   * [StreamingMethod.DIRECT_PLAY_ONLY] for now; intended to default to [StreamingMethod.AUTO]
   * once the HLS route has proven itself in the wild.
   */
  var streamingMethod: StreamingMethod
  fun observeStreamingMethod(): StateFlow<StreamingMethod>

  /**
   * When true, resuming playback after a pause rewinds by an amount that scales with how long playback was
   * paused, per the sliding window derived from [resumeRewindConfig].
   */
  var autoRewindOnResumeEnabled: Boolean
  fun observeAutoRewindOnResumeEnabled(): StateFlow<Boolean>

  /**
   * The configuration (min pause floor + rewind range) from which the auto-rewind sliding window is derived.
   * See [ResumeRewindConfig], [ResumeRewindConfig.tiers], and [rewindForPause].
   */
  var resumeRewindConfig: ResumeRewindConfig
  fun observeResumeRewindConfig(): StateFlow<ResumeRewindConfig>

  /**
   * When true, an auto-rewind on resume never crosses back past the start of the current chapter — if the
   * rewind would go before the chapter boundary, it stops at the boundary instead.
   */
  var autoRewindStopAtChapterBoundary: Boolean
  fun observeAutoRewindStopAtChapterBoundary(): StateFlow<Boolean>

  /**
   * Transient, persisted marker for a pause that may still owe a rewind on resume. Persisted so that a pause
   * interrupted by the app being killed still rewinds when playback resumes. Null when no pause is pending.
   */
  var pendingResumeRewind: PendingResumeRewind?

  /**
   * When true, the book's overall time will display in a progress bar in
   * in the playback ui.
   */
  var bookTimeInPlaybackUi: Boolean
  fun observeBookTimeInPlaybackUi(): StateFlow<Boolean>

  /**
   * When true, the playback slider will be wavy
   */
  var playbackWavyScrubber: Boolean
  fun observePlaybackWavyScrubber(): StateFlow<Boolean>
}

/** The configurable bounds for the metered/unmetered listening-sync intervals. */
val SyncIntervalRange: ClosedRange<Duration> = 5.seconds..5.minutes
