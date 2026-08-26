// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlin.time.Duration
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
   * When true (the default), streaming playback opportunistically opens a server playback
   * session and reports listening in real time through it. When false, every session is
   * locally owned and syncs after the fact — the pre-server-session behavior, and the
   * escape hatch if server-session accounting misbehaves.
   */
  var serverSessionsEnabled: Boolean
  fun observeServerSessionsEnabled(): StateFlow<Boolean>

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
