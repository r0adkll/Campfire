// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.PendingResumeRewind
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.ResumeRewindConfig
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = PlaybackSettings::class)
@Inject
class PlaybackSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : PlaybackSettings, AppSettings() {

  private val enableMp3IndexSeekingProperty = booleanSetting(PREF_MP3_SEEKING)
  override var enableMp3IndexSeeking: Boolean by enableMp3IndexSeekingProperty
  override fun observeMp3IndexSeeking(): StateFlow<Boolean> = enableMp3IndexSeekingProperty.observe()

  private val forwardTimeMsProperty = longSetting(PREF_FORWARD_TIME_MS, DEFAULT_FORWARD_TIME_MS)
  override var forwardTimeMs: Long by forwardTimeMsProperty
  override fun observeForwardTimeMs(): StateFlow<Long> = forwardTimeMsProperty.observe()

  private val backwardTimeMsProperty = longSetting(PREF_BACKWARD_TIME_MS, DEFAULT_BACKWARD_TIME_MS)
  override var backwardTimeMs: Long by backwardTimeMsProperty
  override fun observeBackwardTimeMs(): StateFlow<Long> = backwardTimeMsProperty.observe()

  private val trackResetThresholdProperty = durationSetting(
    key = PREF_TRACK_RESET_THRESHOLD,
    defaultValue = DEFAULT_TRACK_RESET_THRESHOLD_SECONDS.seconds,
  )
  override var trackResetThreshold: Duration by trackResetThresholdProperty
  override fun observeTrackResetThreshold(): StateFlow<Duration> = trackResetThresholdProperty.observe()

  private val playbackRatesProperty = customSetting(
    key = PREF_PLAYBACK_RATES,
    defaultValue = DEFAULT_PLAYBACK_RATES,
    getter = { it.asFloatList() },
    setter = { rates -> rates.joinToString(PLAYBACK_RATES_SEPARATOR) },
  )
  override var playbackRates: List<Float> by playbackRatesProperty
  override fun observePlaybackRates(): StateFlow<List<Float>> = playbackRatesProperty.observe()

  override var playbackSpeed: Float by floatSetting(PREF_PLAYBACK_SPEED, DEFAULT_PLAYBACK_SPEED)

  private val remoteNextPrevSkipsChaptersProperty = booleanSetting(
    PREF_REMOTE_NEXT_PREV_SKIPS_CHAPTERS,
    DEFAULT_REMOTE_NEXT_PREV_SKIPS_CHAPTERS,
  )
  override var remoteNextPrevSkipsChapters: Boolean by remoteNextPrevSkipsChaptersProperty
  override fun observeRemoteNextPrevSkipsChapters(): StateFlow<Boolean> = remoteNextPrevSkipsChaptersProperty.observe()

  private val syncEnabledProperty = booleanSetting(PREF_SYNC, DEFAULT_AUTO_SYNC)
  override var syncEnabled: Boolean by syncEnabledProperty
  override fun observeSyncEnabled(): StateFlow<Boolean> = syncEnabledProperty.observe()

  private val autoSyncEnabledProperty = booleanSetting(PREF_AUTO_SYNC, DEFAULT_AUTO_SYNC)
  override var autoSyncEnabled: Boolean by autoSyncEnabledProperty
  override fun observeAutoSyncEnabled(): StateFlow<Boolean> = autoSyncEnabledProperty.observe()

  private val playbackHistoryEnabledProperty = booleanSetting(PREF_PLAYBACK_HISTORY, DEFAULT_PLAYBACK_HISTORY)
  override var playbackHistoryEnabled: Boolean by playbackHistoryEnabledProperty
  override fun observePlaybackHistoryEnabled(): StateFlow<Boolean> = playbackHistoryEnabledProperty.observe()

  private val serverSessionsEnabledProperty = booleanSetting(PREF_SERVER_SESSIONS, DEFAULT_SERVER_SESSIONS)
  override var serverSessionsEnabled: Boolean by serverSessionsEnabledProperty
  override fun observeServerSessionsEnabled(): StateFlow<Boolean> = serverSessionsEnabledProperty.observe()

  private val autoRewindOnResumeEnabledProperty = booleanSetting(
    PREF_AUTO_REWIND_ON_RESUME,
    DEFAULT_AUTO_REWIND_ON_RESUME,
  )
  override var autoRewindOnResumeEnabled: Boolean by autoRewindOnResumeEnabledProperty
  override fun observeAutoRewindOnResumeEnabled(): StateFlow<Boolean> = autoRewindOnResumeEnabledProperty.observe()

  private val minPauseThresholdProperty = durationSetting(
    PREF_MIN_PAUSE_THRESHOLD,
    ResumeRewindConfig.Default.minPauseThreshold,
  )
  private var minPauseThreshold: Duration by minPauseThresholdProperty

  private val minResumeRewindProperty = durationSetting(PREF_MIN_RESUME_REWIND, ResumeRewindConfig.Default.minRewind)
  private var minResumeRewind: Duration by minResumeRewindProperty

  private val maxResumeRewindProperty = durationSetting(PREF_MAX_RESUME_REWIND, ResumeRewindConfig.Default.maxRewind)
  private var maxResumeRewind: Duration by maxResumeRewindProperty

  override var resumeRewindConfig: ResumeRewindConfig
    get() = ResumeRewindConfig(minPauseThreshold, minResumeRewind, maxResumeRewind)
    set(value) {
      minPauseThreshold = value.minPauseThreshold
      minResumeRewind = value.minRewind
      maxResumeRewind = value.maxRewind
    }

  override fun observeResumeRewindConfig(): StateFlow<ResumeRewindConfig> = combine(
    minPauseThresholdProperty.observe(),
    minResumeRewindProperty.observe(),
    maxResumeRewindProperty.observe(),
  ) { minPause, minRewind, maxRewind ->
    ResumeRewindConfig(minPause, minRewind, maxRewind)
  }.stateIn(scope, SharingStarted.Lazily, resumeRewindConfig)

  private val autoRewindStopAtChapterBoundaryProperty = booleanSetting(
    PREF_AUTO_REWIND_STOP_AT_CHAPTER,
    DEFAULT_AUTO_REWIND_STOP_AT_CHAPTER,
  )
  override var autoRewindStopAtChapterBoundary: Boolean by autoRewindStopAtChapterBoundaryProperty
  override fun observeAutoRewindStopAtChapterBoundary(): StateFlow<Boolean> =
    autoRewindStopAtChapterBoundaryProperty.observe()

  override var pendingResumeRewind: PendingResumeRewind?
    get() = settings.getStringOrNull(PREF_PENDING_RESUME_REWIND)?.toPendingResumeRewind()
    set(value) {
      if (value == null) {
        settings.remove(PREF_PENDING_RESUME_REWIND)
      } else {
        settings.putString(PREF_PENDING_RESUME_REWIND, value.serialize())
      }
    }

  private val bookTimeInPlaybackUiProperty = booleanSetting(
    PREF_BOOK_TIME_UI,
    false,
  )
  override var bookTimeInPlaybackUi: Boolean by bookTimeInPlaybackUiProperty
  override fun observeBookTimeInPlaybackUi(): StateFlow<Boolean> =
    bookTimeInPlaybackUiProperty.observe()

  private val playbackWavyScrubberProperty = booleanSetting(
    PREF_WAVY_SLIDER,
    true,
  )
  override var playbackWavyScrubber: Boolean by playbackWavyScrubberProperty
  override fun observePlaybackWavyScrubber(): StateFlow<Boolean> =
    playbackWavyScrubberProperty.observe()

  private fun String.asFloatList(): List<Float> = split(PLAYBACK_RATES_SEPARATOR).mapNotNull { it.toFloatOrNull() }

  private fun PendingResumeRewind.serialize(): String =
    "$pausedAtEpochMillis$PENDING_RESUME_REWIND_SEPARATOR$libraryItemId"

  private fun String.toPendingResumeRewind(): PendingResumeRewind? {
    val epochMillis = substringBefore(PENDING_RESUME_REWIND_SEPARATOR).toLongOrNull() ?: return null
    val libraryItemId = substringAfter(PENDING_RESUME_REWIND_SEPARATOR, "").ifEmpty { return null }
    return PendingResumeRewind(epochMillis, libraryItemId)
  }
}

internal const val PREF_MP3_SEEKING = "pref_playback_mp3_seeking"
internal const val PREF_FORWARD_TIME_MS = "pref_playback_forward_time_ms"
internal const val PREF_BACKWARD_TIME_MS = "pref_playback_backward_time_ms"
internal const val PREF_TRACK_RESET_THRESHOLD = "pref_playback_track_reset_threshold"
internal const val PREF_PLAYBACK_RATES = "pref_playback_rates"
internal const val PREF_PLAYBACK_SPEED = "pref_playback_speed"
internal const val PREF_SYNC = "pref_synchronization"
internal const val PREF_AUTO_SYNC = "pref_auto_sync"
internal const val PREF_REMOTE_NEXT_PREV_SKIPS_CHAPTERS = "pref_playback_remote_next_prev_skips_chapters"
internal const val PREF_PLAYBACK_HISTORY = "pref_playback_history_enabled"
internal const val PREF_SERVER_SESSIONS = "pref_server_sessions_enabled"
internal const val PREF_MIN_PAUSE_THRESHOLD = "pref_playback_resume_rewind_min_pause_threshold"
internal const val PREF_MIN_RESUME_REWIND = "pref_playback_resume_rewind_min"
internal const val PREF_MAX_RESUME_REWIND = "pref_playback_resume_rewind_max"
internal const val PREF_PENDING_RESUME_REWIND = "pref_playback_pending_resume_rewind"
internal const val PENDING_RESUME_REWIND_SEPARATOR = "|"
internal const val PREF_AUTO_REWIND_STOP_AT_CHAPTER = "pref_playback_auto_rewind_stop_at_chapter"
internal const val PREF_AUTO_REWIND_ON_RESUME = "pref_playback_auto_rewind_on_resume"
internal const val PREF_BOOK_TIME_UI = "pref_book_time_playback_ui"
internal const val PREF_WAVY_SLIDER = "pref_wavy_playback_slider"

internal const val PLAYBACK_RATES_SEPARATOR = "::"

internal const val DEFAULT_FORWARD_TIME_MS = 30L * 1000L // 30s
internal const val DEFAULT_BACKWARD_TIME_MS = 10L * 1000L // 15s
internal const val DEFAULT_TRACK_RESET_THRESHOLD_SECONDS = 5.0 // 5s
internal val DEFAULT_PLAYBACK_RATES = listOf(1f, 1.1f, 1.25f, 1.5f, 2f)
internal const val DEFAULT_PLAYBACK_SPEED = 1f
internal const val DEFAULT_REMOTE_NEXT_PREV_SKIPS_CHAPTERS = false
internal const val DEFAULT_AUTO_SYNC = true
internal const val DEFAULT_PLAYBACK_HISTORY = true
internal const val DEFAULT_SERVER_SESSIONS = true
internal const val DEFAULT_AUTO_REWIND_ON_RESUME = false
internal const val DEFAULT_AUTO_REWIND_STOP_AT_CHAPTER = true
