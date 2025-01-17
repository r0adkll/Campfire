package app.campfire.common.settings

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
}
