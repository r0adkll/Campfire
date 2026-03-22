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
}
