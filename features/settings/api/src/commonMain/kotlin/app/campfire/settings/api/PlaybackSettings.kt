// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.model.LibraryItemId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface PlaybackSettings {

  fun setEnableMp3IndexSeeking(value: Boolean)
  fun observeMp3IndexSeeking(): Flow<Boolean>

  fun setForwardTimeMs(value: Long)
  fun setBackwardTimeMs(value: Long)
  fun observeForwardTimeMs(): Flow<Long>
  fun observeBackwardTimeMs(): Flow<Long>

  fun setTrackResetThreshold(value: Duration)
  fun observeTrackResetThreshold(): Flow<Duration>

  fun setPlaybackRates(value: List<Float>)
  fun observePlaybackRates(): Flow<List<Float>>

  fun observePlaybackSpeed(): Flow<Float>
  fun setPlaybackSpeed(value: Float)

  /**
   * Per-item playback speed overrides, keyed by library item id. The presence of an entry means the
   * item has a per-item speed enabled, and its value is that item's saved speed. Items without an
   * entry use the global playback speed.
   */
  fun setItemPlaybackSpeeds(value: Map<LibraryItemId, Float>)
  fun observeItemPlaybackSpeeds(): Flow<Map<LibraryItemId, Float>>

  /**
   * The effective playback speed for [itemId] — its per-item override if one is enabled,
   * otherwise the global playback speed.
   */
  suspend fun playbackSpeedFor(itemId: LibraryItemId?): Float {
    return itemId?.let { observeItemPlaybackSpeeds().first()[it] } ?: observePlaybackSpeed().first()
  }

  /**
   * Persist [speed] to [itemId]'s per-item override when one is enabled, otherwise to the
   * global playback speed.
   */
  fun setPlaybackSpeedFor(itemId: LibraryItemId?, speed: Float)

  /**
   * When true, remote control next/previous buttons skip to next/previous chapter.
   * When false, they seek forward/backward by the configured time.
   */
  fun setRemoteNextPrevSkipsChapters(value: Boolean)
  fun observeRemoteNextPrevSkipsChapters(): Flow<Boolean>

  /**
   * When true, we will show sync opportunities to the user (or allow auto-sync if enabled)
   */
  fun setSyncEnabled(value: Boolean)
  fun observeSyncEnabled(): Flow<Boolean>

  /**
   * When true, a new session will use the media progress if it is newer than the previous session when resuming playback.
   * When false, it will continue to use the local session progress
   */
  fun setAutoSyncEnabled(value: Boolean)
  fun observeAutoSyncEnabled(): Flow<Boolean>

  /**
   * When true, playback actions (play, pause, seek, etc.) will be recorded to a local history.
   * When false, playback history is disabled and all existing history is cleared.
   */
  fun setPlaybackHistoryEnabled(value: Boolean)
  fun observePlaybackHistoryEnabled(): Flow<Boolean>

  /**
   * The minimum interval between listening syncs to the server while on an unmetered
   * connection (Wi-Fi). Constrained to [SyncIntervalRange].
   */
  fun setSyncIntervalUnmetered(value: Duration)
  fun observeSyncIntervalUnmetered(): Flow<Duration>

  /**
   * The minimum interval between listening syncs to the server while on a metered
   * connection (mobile data). Constrained to [SyncIntervalRange].
   */
  fun setSyncIntervalMetered(value: Duration)
  fun observeSyncIntervalMetered(): Flow<Duration>

  /**
   * How streamed items are delivered — see [StreamingMethod]. Defaults to
   * [StreamingMethod.DIRECT_PLAY_ONLY] for now; intended to default to [StreamingMethod.AUTO]
   * once the HLS route has proven itself in the wild.
   */
  fun setStreamingMethod(value: StreamingMethod)
  fun observeStreamingMethod(): Flow<StreamingMethod>

  /**
   * When true, resuming playback after a pause rewinds by an amount that scales with how long playback was
   * paused, per the sliding window derived from the [ResumeRewindConfig].
   */
  fun setAutoRewindOnResumeEnabled(value: Boolean)
  fun observeAutoRewindOnResumeEnabled(): Flow<Boolean>

  /**
   * The configuration (min pause floor + rewind range) from which the auto-rewind sliding window is derived.
   * See [ResumeRewindConfig], [ResumeRewindConfig.tiers], and [rewindForPause].
   */
  fun setResumeRewindConfig(value: ResumeRewindConfig)
  fun observeResumeRewindConfig(): Flow<ResumeRewindConfig>

  /**
   * When true, an auto-rewind on resume never crosses back past the start of the current chapter — if the
   * rewind would go before the chapter boundary, it stops at the boundary instead.
   */
  fun setAutoRewindStopAtChapterBoundary(value: Boolean)
  fun observeAutoRewindStopAtChapterBoundary(): Flow<Boolean>

  /**
   * Transient, persisted marker for a pause that may still owe a rewind on resume. Persisted so that a pause
   * interrupted by the app being killed still rewinds when playback resumes. Null when no pause is pending.
   */
  fun observePendingResumeRewind(): Flow<PendingResumeRewind?>
  fun setPendingResumeRewind(value: PendingResumeRewind?)

  /**
   * When true, the book's overall time will display in a progress bar in
   * in the playback ui.
   */
  fun setBookTimeInPlaybackUi(value: Boolean)
  fun observeBookTimeInPlaybackUi(): Flow<Boolean>

  /**
   * When true, the playback slider will be wavy
   */
  fun setPlaybackWavyScrubber(value: Boolean)
  fun observePlaybackWavyScrubber(): Flow<Boolean>

  /**
   * When true, chapter and book titles too long for the player scroll a few times; otherwise they
   * are ellipsized.
   */
  fun setScrollingTitles(value: Boolean)
  fun observeScrollingTitles(): Flow<Boolean>

  companion object {
    val DefaultForwardTime: Duration = 30.seconds
    val DefaultBackwardTime: Duration = 10.seconds
    val DefaultTrackResetThreshold: Duration = 5.seconds
    val DefaultPlaybackRates: List<Float> = listOf(1f, 1.1f, 1.25f, 1.5f, 2f)
    const val DEFAULT_PLAYBACK_SPEED: Float = 1f
    const val DEFAULT_REMOTE_NEXT_PREV_SKIPS_CHAPTERS: Boolean = false
    const val DEFAULT_MP3_INDEX_SEEKING: Boolean = false
    const val DEFAULT_SYNC_ENABLED: Boolean = true
    const val DEFAULT_PLAYBACK_HISTORY_ENABLED: Boolean = true
    const val DEFAULT_BOOK_TIME_IN_PLAYBACK_UI: Boolean = false
    const val DEFAULT_PLAYBACK_WAVY_SCRUBBER: Boolean = true
    const val DEFAULT_SCROLLING_TITLES: Boolean = true
  }
}

/** The configurable bounds for the metered/unmetered listening-sync intervals. */
val SyncIntervalRange: ClosedRange<Duration> = 5.seconds..5.minutes
