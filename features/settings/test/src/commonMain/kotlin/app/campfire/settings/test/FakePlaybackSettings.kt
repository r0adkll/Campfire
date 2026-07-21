package app.campfire.settings.test

import app.campfire.settings.api.PendingResumeRewind
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.ResumeRewindConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A simple in-memory [PlaybackSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakePlaybackSettings : PlaybackSettings {

  private val _enableMp3IndexSeeking = MutableStateFlow(false)
  override var enableMp3IndexSeeking: Boolean
    get() = _enableMp3IndexSeeking.value
    set(value) { _enableMp3IndexSeeking.value = value }
  override fun observeMp3IndexSeeking(): StateFlow<Boolean> = _enableMp3IndexSeeking.asStateFlow()

  private val _forwardTimeMs = MutableStateFlow(30_000L)
  override var forwardTimeMs: Long
    get() = _forwardTimeMs.value
    set(value) { _forwardTimeMs.value = value }
  override fun observeForwardTimeMs(): StateFlow<Long> = _forwardTimeMs.asStateFlow()

  private val _backwardTimeMs = MutableStateFlow(10_000L)
  override var backwardTimeMs: Long
    get() = _backwardTimeMs.value
    set(value) { _backwardTimeMs.value = value }
  override fun observeBackwardTimeMs(): StateFlow<Long> = _backwardTimeMs.asStateFlow()

  private val _trackResetThreshold = MutableStateFlow(5.seconds)
  override var trackResetThreshold: Duration
    get() = _trackResetThreshold.value
    set(value) { _trackResetThreshold.value = value }
  override fun observeTrackResetThreshold(): StateFlow<Duration> = _trackResetThreshold.asStateFlow()

  private val _playbackRates = MutableStateFlow(listOf(1f, 1.1f, 1.25f, 1.5f, 2f))
  override var playbackRates: List<Float>
    get() = _playbackRates.value
    set(value) { _playbackRates.value = value }
  override fun observePlaybackRates(): StateFlow<List<Float>> = _playbackRates.asStateFlow()

  override var playbackSpeed: Float = 1f

  private val _remoteNextPrevSkipsChapters = MutableStateFlow(true)
  override var remoteNextPrevSkipsChapters: Boolean
    get() = _remoteNextPrevSkipsChapters.value
    set(value) { _remoteNextPrevSkipsChapters.value = value }
  override fun observeRemoteNextPrevSkipsChapters(): StateFlow<Boolean> = _remoteNextPrevSkipsChapters.asStateFlow()

  private val _syncEnabled = MutableStateFlow(true)
  override var syncEnabled: Boolean
    get() = _syncEnabled.value
    set(value) { _syncEnabled.value = value }
  override fun observeSyncEnabled(): StateFlow<Boolean> = _syncEnabled.asStateFlow()

  private val _autoSyncEnabled = MutableStateFlow(true)
  override var autoSyncEnabled: Boolean
    get() = _autoSyncEnabled.value
    set(value) { _autoSyncEnabled.value = value }
  override fun observeAutoSyncEnabled(): StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

  private val _playbackHistoryEnabled = MutableStateFlow(true)
  override var playbackHistoryEnabled: Boolean
    get() = _playbackHistoryEnabled.value
    set(value) { _playbackHistoryEnabled.value = value }
  override fun observePlaybackHistoryEnabled(): StateFlow<Boolean> = _playbackHistoryEnabled.asStateFlow()

  private val _autoRewindOnResumeEnabled = MutableStateFlow(false)
  override var autoRewindOnResumeEnabled: Boolean
    get() = _autoRewindOnResumeEnabled.value
    set(value) { _autoRewindOnResumeEnabled.value = value }
  override fun observeAutoRewindOnResumeEnabled(): StateFlow<Boolean> = _autoRewindOnResumeEnabled.asStateFlow()

  private val _resumeRewindConfig = MutableStateFlow(ResumeRewindConfig.Default)
  override var resumeRewindConfig: ResumeRewindConfig
    get() = _resumeRewindConfig.value
    set(value) { _resumeRewindConfig.value = value }
  override fun observeResumeRewindConfig(): StateFlow<ResumeRewindConfig> = _resumeRewindConfig.asStateFlow()

  private val _autoRewindStopAtChapterBoundary = MutableStateFlow(true)
  override var autoRewindStopAtChapterBoundary: Boolean
    get() = _autoRewindStopAtChapterBoundary.value
    set(value) { _autoRewindStopAtChapterBoundary.value = value }
  override fun observeAutoRewindStopAtChapterBoundary(): StateFlow<Boolean> =
    _autoRewindStopAtChapterBoundary.asStateFlow()

  override var pendingResumeRewind: PendingResumeRewind? = null
}
