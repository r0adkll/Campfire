// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.PendingResumeRewind
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.ResumeRewindConfig
import app.campfire.settings.api.StreamingMethod
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [PlaybackSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakePlaybackSettings : PlaybackSettings {

  private val _enableMp3IndexSeeking = MutableStateFlow<Boolean>(false)
  val enableMp3IndexSeeking: Boolean get() = _enableMp3IndexSeeking.value
  override fun setEnableMp3IndexSeeking(value: Boolean) {
    _enableMp3IndexSeeking.value = value
  }
  override fun observeMp3IndexSeeking(): StateFlow<Boolean> = _enableMp3IndexSeeking.asStateFlow()

  private val _forwardTimeMs = MutableStateFlow<Long>(30_000L)
  val forwardTimeMs: Long get() = _forwardTimeMs.value
  override fun setForwardTimeMs(value: Long) {
    _forwardTimeMs.value = value
  }
  override fun observeForwardTimeMs(): StateFlow<Long> = _forwardTimeMs.asStateFlow()

  private val _backwardTimeMs = MutableStateFlow<Long>(10_000L)
  val backwardTimeMs: Long get() = _backwardTimeMs.value
  override fun setBackwardTimeMs(value: Long) {
    _backwardTimeMs.value = value
  }
  override fun observeBackwardTimeMs(): StateFlow<Long> = _backwardTimeMs.asStateFlow()

  private val _trackResetThreshold = MutableStateFlow<Duration>(5.seconds)
  val trackResetThreshold: Duration get() = _trackResetThreshold.value
  override fun setTrackResetThreshold(value: Duration) {
    _trackResetThreshold.value = value
  }
  override fun observeTrackResetThreshold(): StateFlow<Duration> = _trackResetThreshold.asStateFlow()

  private val _playbackRates = MutableStateFlow<List<Float>>(listOf(1f, 1.1f, 1.25f, 1.5f, 2f))
  val playbackRates: List<Float> get() = _playbackRates.value
  override fun setPlaybackRates(value: List<Float>) {
    _playbackRates.value = value
  }
  override fun observePlaybackRates(): StateFlow<List<Float>> = _playbackRates.asStateFlow()

  private val _playbackSpeed = MutableStateFlow<Float>(1f)
  val playbackSpeed: Float get() = _playbackSpeed.value
  override fun setPlaybackSpeed(value: Float) {
    _playbackSpeed.value = value
  }
  override fun observePlaybackSpeed(): StateFlow<Float> = _playbackSpeed.asStateFlow()

  private val _remoteNextPrevSkipsChapters = MutableStateFlow<Boolean>(true)
  val remoteNextPrevSkipsChapters: Boolean get() = _remoteNextPrevSkipsChapters.value
  override fun setRemoteNextPrevSkipsChapters(value: Boolean) {
    _remoteNextPrevSkipsChapters.value = value
  }
  override fun observeRemoteNextPrevSkipsChapters(): StateFlow<Boolean> = _remoteNextPrevSkipsChapters.asStateFlow()

  private val _syncEnabled = MutableStateFlow<Boolean>(true)
  val syncEnabled: Boolean get() = _syncEnabled.value
  override fun setSyncEnabled(value: Boolean) {
    _syncEnabled.value = value
  }
  override fun observeSyncEnabled(): StateFlow<Boolean> = _syncEnabled.asStateFlow()

  private val _autoSyncEnabled = MutableStateFlow<Boolean>(true)
  val autoSyncEnabled: Boolean get() = _autoSyncEnabled.value
  override fun setAutoSyncEnabled(value: Boolean) {
    _autoSyncEnabled.value = value
  }
  override fun observeAutoSyncEnabled(): StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

  private val _playbackHistoryEnabled = MutableStateFlow<Boolean>(true)
  val playbackHistoryEnabled: Boolean get() = _playbackHistoryEnabled.value
  override fun setPlaybackHistoryEnabled(value: Boolean) {
    _playbackHistoryEnabled.value = value
  }
  override fun observePlaybackHistoryEnabled(): StateFlow<Boolean> = _playbackHistoryEnabled.asStateFlow()

  private val _syncIntervalUnmetered = MutableStateFlow<Duration>(15.seconds)
  val syncIntervalUnmetered: Duration get() = _syncIntervalUnmetered.value
  override fun setSyncIntervalUnmetered(value: Duration) {
    _syncIntervalUnmetered.value = value
  }
  override fun observeSyncIntervalUnmetered(): StateFlow<Duration> = _syncIntervalUnmetered.asStateFlow()

  private val _syncIntervalMetered = MutableStateFlow<Duration>(60.seconds)
  val syncIntervalMetered: Duration get() = _syncIntervalMetered.value
  override fun setSyncIntervalMetered(value: Duration) {
    _syncIntervalMetered.value = value
  }
  override fun observeSyncIntervalMetered(): StateFlow<Duration> = _syncIntervalMetered.asStateFlow()

  private val _streamingMethod = MutableStateFlow<StreamingMethod>(StreamingMethod.DIRECT_PLAY_ONLY)
  val streamingMethod: StreamingMethod get() = _streamingMethod.value
  override fun setStreamingMethod(value: StreamingMethod) {
    _streamingMethod.value = value
  }
  override fun observeStreamingMethod(): StateFlow<StreamingMethod> = _streamingMethod.asStateFlow()

  private val _autoRewindOnResumeEnabled = MutableStateFlow<Boolean>(false)
  val autoRewindOnResumeEnabled: Boolean get() = _autoRewindOnResumeEnabled.value
  override fun setAutoRewindOnResumeEnabled(value: Boolean) {
    _autoRewindOnResumeEnabled.value = value
  }
  override fun observeAutoRewindOnResumeEnabled(): StateFlow<Boolean> = _autoRewindOnResumeEnabled.asStateFlow()

  private val _resumeRewindConfig = MutableStateFlow<ResumeRewindConfig>(ResumeRewindConfig.Default)
  val resumeRewindConfig: ResumeRewindConfig get() = _resumeRewindConfig.value
  override fun setResumeRewindConfig(value: ResumeRewindConfig) {
    _resumeRewindConfig.value = value
  }
  override fun observeResumeRewindConfig(): StateFlow<ResumeRewindConfig> = _resumeRewindConfig.asStateFlow()

  private val _autoRewindStopAtChapterBoundary = MutableStateFlow<Boolean>(true)
  val autoRewindStopAtChapterBoundary: Boolean get() = _autoRewindStopAtChapterBoundary.value
  override fun setAutoRewindStopAtChapterBoundary(value: Boolean) {
    _autoRewindStopAtChapterBoundary.value = value
  }
  override fun observeAutoRewindStopAtChapterBoundary(): StateFlow<Boolean> =
    _autoRewindStopAtChapterBoundary.asStateFlow()

  private val _pendingResumeRewind = MutableStateFlow<PendingResumeRewind?>(null)
  val pendingResumeRewind: PendingResumeRewind? get() = _pendingResumeRewind.value
  override fun setPendingResumeRewind(value: PendingResumeRewind?) {
    _pendingResumeRewind.value = value
  }
  override fun observePendingResumeRewind(): StateFlow<PendingResumeRewind?> = _pendingResumeRewind.asStateFlow()

  private val _bookTimeInPlaybackUi = MutableStateFlow<Boolean>(false)
  val bookTimeInPlaybackUi: Boolean get() = _bookTimeInPlaybackUi.value
  override fun setBookTimeInPlaybackUi(value: Boolean) {
    _bookTimeInPlaybackUi.value = value
  }
  override fun observeBookTimeInPlaybackUi(): StateFlow<Boolean> = _bookTimeInPlaybackUi.asStateFlow()

  private val _playbackWavyScrubber = MutableStateFlow<Boolean>(true)
  val playbackWavyScrubber: Boolean get() = _playbackWavyScrubber.value
  override fun setPlaybackWavyScrubber(value: Boolean) {
    _playbackWavyScrubber.value = value
  }
  override fun observePlaybackWavyScrubber(): StateFlow<Boolean> = _playbackWavyScrubber.asStateFlow()

  private val _scrollingTitles = MutableStateFlow<Boolean>(true)
  val scrollingTitles: Boolean get() = _scrollingTitles.value
  override fun setScrollingTitles(value: Boolean) {
    _scrollingTitles.value = value
  }
  override fun observeScrollingTitles(): StateFlow<Boolean> = _scrollingTitles.asStateFlow()
}
