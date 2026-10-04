// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.playback

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.LibraryItemId
import app.campfire.settings.api.PendingResumeRewind
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.ResumeRewindConfig
import app.campfire.settings.api.StreamingMethod
import app.campfire.settings.store.AppSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<PlaybackSettings>())
@Inject
class PlaybackSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : PlaybackSettings, AppSettings() {

  private val enableMp3IndexSeekingProperty = booleanSetting(PREF_MP3_SEEKING)
  override val enableMp3IndexSeeking: Boolean by enableMp3IndexSeekingProperty
  override fun setEnableMp3IndexSeeking(value: Boolean) = enableMp3IndexSeekingProperty.set(value)
  override fun observeMp3IndexSeeking(): StateFlow<Boolean> = enableMp3IndexSeekingProperty.observe()

  private val forwardTimeMsProperty = longSetting(PREF_FORWARD_TIME_MS, DEFAULT_FORWARD_TIME_MS)
  override val forwardTimeMs: Long by forwardTimeMsProperty
  override fun setForwardTimeMs(value: Long) = forwardTimeMsProperty.set(value)
  override fun observeForwardTimeMs(): StateFlow<Long> = forwardTimeMsProperty.observe()

  private val backwardTimeMsProperty = longSetting(PREF_BACKWARD_TIME_MS, DEFAULT_BACKWARD_TIME_MS)
  override val backwardTimeMs: Long by backwardTimeMsProperty
  override fun setBackwardTimeMs(value: Long) = backwardTimeMsProperty.set(value)
  override fun observeBackwardTimeMs(): StateFlow<Long> = backwardTimeMsProperty.observe()

  private val trackResetThresholdProperty = durationSetting(
    key = PREF_TRACK_RESET_THRESHOLD,
    defaultValue = DEFAULT_TRACK_RESET_THRESHOLD_SECONDS.seconds,
  )
  override val trackResetThreshold: Duration by trackResetThresholdProperty
  override fun setTrackResetThreshold(value: Duration) = trackResetThresholdProperty.set(value)
  override fun observeTrackResetThreshold(): StateFlow<Duration> = trackResetThresholdProperty.observe()

  private val playbackRatesProperty = customSetting(
    key = PREF_PLAYBACK_RATES,
    defaultValue = DEFAULT_PLAYBACK_RATES,
    getter = { it.asFloatList() },
    setter = { rates -> rates.joinToString(PLAYBACK_RATES_SEPARATOR) },
  )
  override val playbackRates: List<Float> by playbackRatesProperty
  override fun setPlaybackRates(value: List<Float>) = playbackRatesProperty.set(value)
  override fun observePlaybackRates(): StateFlow<List<Float>> = playbackRatesProperty.observe()

  private val playbackSpeedProperty = floatSetting(PREF_PLAYBACK_SPEED, DEFAULT_PLAYBACK_SPEED)
  override val playbackSpeed: Float by playbackSpeedProperty
  override fun setPlaybackSpeed(value: Float) = playbackSpeedProperty.set(value)
  override fun observePlaybackSpeed(): StateFlow<Float> = playbackSpeedProperty.observe()

  private val itemPlaybackSpeedsProperty = customSetting(
    key = PREF_ITEM_PLAYBACK_SPEEDS,
    defaultValue = emptyMap<LibraryItemId, Float>(),
    getter = { it.asItemSpeedMap() },
    setter = { speeds ->
      speeds.entries.joinToString(ITEM_SPEED_ENTRY_SEPARATOR) {
        "${it.key}$ITEM_SPEED_VALUE_SEPARATOR${it.value}"
      }
    },
  )
  override val itemPlaybackSpeeds: Map<LibraryItemId, Float> by itemPlaybackSpeedsProperty
  override fun setItemPlaybackSpeeds(value: Map<LibraryItemId, Float>) = itemPlaybackSpeedsProperty.set(value)
  override fun observeItemPlaybackSpeeds(): StateFlow<Map<LibraryItemId, Float>> =
    itemPlaybackSpeedsProperty.observe()

  private val remoteNextPrevSkipsChaptersProperty = booleanSetting(
    PREF_REMOTE_NEXT_PREV_SKIPS_CHAPTERS,
    DEFAULT_REMOTE_NEXT_PREV_SKIPS_CHAPTERS,
  )
  override val remoteNextPrevSkipsChapters: Boolean by remoteNextPrevSkipsChaptersProperty
  override fun setRemoteNextPrevSkipsChapters(value: Boolean) = remoteNextPrevSkipsChaptersProperty.set(value)
  override fun observeRemoteNextPrevSkipsChapters(): StateFlow<Boolean> = remoteNextPrevSkipsChaptersProperty.observe()

  private val syncEnabledProperty = booleanSetting(PREF_SYNC, DEFAULT_AUTO_SYNC)
  override val syncEnabled: Boolean by syncEnabledProperty
  override fun setSyncEnabled(value: Boolean) = syncEnabledProperty.set(value)
  override fun observeSyncEnabled(): StateFlow<Boolean> = syncEnabledProperty.observe()

  private val autoSyncEnabledProperty = booleanSetting(PREF_AUTO_SYNC, DEFAULT_AUTO_SYNC)
  override val autoSyncEnabled: Boolean by autoSyncEnabledProperty
  override fun setAutoSyncEnabled(value: Boolean) = autoSyncEnabledProperty.set(value)
  override fun observeAutoSyncEnabled(): StateFlow<Boolean> = autoSyncEnabledProperty.observe()

  private val playbackHistoryEnabledProperty = booleanSetting(PREF_PLAYBACK_HISTORY, DEFAULT_PLAYBACK_HISTORY)
  override val playbackHistoryEnabled: Boolean by playbackHistoryEnabledProperty
  override fun setPlaybackHistoryEnabled(value: Boolean) = playbackHistoryEnabledProperty.set(value)
  override fun observePlaybackHistoryEnabled(): StateFlow<Boolean> = playbackHistoryEnabledProperty.observe()

  private val syncIntervalUnmeteredProperty =
    durationSetting(PREF_SYNC_INTERVAL_UNMETERED, DEFAULT_SYNC_INTERVAL_UNMETERED)
  override val syncIntervalUnmetered: Duration by syncIntervalUnmeteredProperty
  override fun setSyncIntervalUnmetered(value: Duration) = syncIntervalUnmeteredProperty.set(value)
  override fun observeSyncIntervalUnmetered(): StateFlow<Duration> = syncIntervalUnmeteredProperty.observe()

  private val syncIntervalMeteredProperty = durationSetting(PREF_SYNC_INTERVAL_METERED, DEFAULT_SYNC_INTERVAL_METERED)
  override val syncIntervalMetered: Duration by syncIntervalMeteredProperty
  override fun setSyncIntervalMetered(value: Duration) = syncIntervalMeteredProperty.set(value)
  override fun observeSyncIntervalMetered(): StateFlow<Duration> = syncIntervalMeteredProperty.observe()

  private val streamingMethodProperty = enumSetting(PREF_STREAMING_METHOD, StreamingMethod)
  override val streamingMethod: StreamingMethod by streamingMethodProperty
  override fun setStreamingMethod(value: StreamingMethod) = streamingMethodProperty.set(value)
  override fun observeStreamingMethod(): StateFlow<StreamingMethod> = streamingMethodProperty.observe()

  private val autoRewindOnResumeEnabledProperty = booleanSetting(
    PREF_AUTO_REWIND_ON_RESUME,
    DEFAULT_AUTO_REWIND_ON_RESUME,
  )
  override val autoRewindOnResumeEnabled: Boolean by autoRewindOnResumeEnabledProperty
  override fun setAutoRewindOnResumeEnabled(value: Boolean) = autoRewindOnResumeEnabledProperty.set(value)
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

  override val resumeRewindConfig: ResumeRewindConfig
    get() = ResumeRewindConfig(minPauseThreshold, minResumeRewind, maxResumeRewind)

  override fun setResumeRewindConfig(value: ResumeRewindConfig) {
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
  override val autoRewindStopAtChapterBoundary: Boolean by autoRewindStopAtChapterBoundaryProperty
  override fun setAutoRewindStopAtChapterBoundary(value: Boolean) = autoRewindStopAtChapterBoundaryProperty.set(value)
  override fun observeAutoRewindStopAtChapterBoundary(): StateFlow<Boolean> =
    autoRewindStopAtChapterBoundaryProperty.observe()

  private val pendingResumeRewindProperty = stringOrNullSetting(PREF_PENDING_RESUME_REWIND)
  override val pendingResumeRewind: PendingResumeRewind?
    get() = settings.getStringOrNull(PREF_PENDING_RESUME_REWIND)?.toPendingResumeRewind()

  override fun setPendingResumeRewind(value: PendingResumeRewind?) =
    pendingResumeRewindProperty.set(value?.serialize())
  override fun observePendingResumeRewind(): StateFlow<PendingResumeRewind?> =
    pendingResumeRewindProperty.observe()
      .map { it?.toPendingResumeRewind() }
      .stateIn(scope, SharingStarted.Lazily, pendingResumeRewind)

  private val bookTimeInPlaybackUiProperty = booleanSetting(
    PREF_BOOK_TIME_UI,
    false,
  )
  override val bookTimeInPlaybackUi: Boolean by bookTimeInPlaybackUiProperty
  override fun setBookTimeInPlaybackUi(value: Boolean) = bookTimeInPlaybackUiProperty.set(value)
  override fun observeBookTimeInPlaybackUi(): StateFlow<Boolean> =
    bookTimeInPlaybackUiProperty.observe()

  private val playbackWavyScrubberProperty = booleanSetting(
    PREF_WAVY_SLIDER,
    true,
  )
  override val playbackWavyScrubber: Boolean by playbackWavyScrubberProperty
  override fun setPlaybackWavyScrubber(value: Boolean) = playbackWavyScrubberProperty.set(value)
  override fun observePlaybackWavyScrubber(): StateFlow<Boolean> =
    playbackWavyScrubberProperty.observe()

  private val scrollingTitlesProperty = booleanSetting(
    PREF_SCROLLING_TITLES,
    true,
  )
  override val scrollingTitles: Boolean by scrollingTitlesProperty
  override fun setScrollingTitles(value: Boolean) = scrollingTitlesProperty.set(value)
  override fun observeScrollingTitles(): StateFlow<Boolean> =
    scrollingTitlesProperty.observe()

  private fun String.asFloatList(): List<Float> = split(PLAYBACK_RATES_SEPARATOR).mapNotNull { it.toFloatOrNull() }

  private fun String.asItemSpeedMap(): Map<LibraryItemId, Float> {
    return split(ITEM_SPEED_ENTRY_SEPARATOR)
      .mapNotNull { entry ->
        val itemId = entry.substringBefore(ITEM_SPEED_VALUE_SEPARATOR)
        val speed = entry.substringAfter(ITEM_SPEED_VALUE_SEPARATOR, "").toFloatOrNull()
        if (itemId.isEmpty() || speed == null) null else itemId to speed
      }
      .toMap()
  }

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
internal const val PREF_ITEM_PLAYBACK_SPEEDS = "pref_item_playback_speeds"
internal const val ITEM_SPEED_ENTRY_SEPARATOR = "::"
internal const val ITEM_SPEED_VALUE_SEPARATOR = "|"
internal const val PREF_SYNC = "pref_synchronization"
internal const val PREF_AUTO_SYNC = "pref_auto_sync"
internal const val PREF_REMOTE_NEXT_PREV_SKIPS_CHAPTERS = "pref_playback_remote_next_prev_skips_chapters"
internal const val PREF_PLAYBACK_HISTORY = "pref_playback_history_enabled"
internal const val PREF_SYNC_INTERVAL_UNMETERED = "pref_sync_interval_unmetered"
internal const val PREF_SYNC_INTERVAL_METERED = "pref_sync_interval_metered"
internal const val PREF_STREAMING_METHOD = "pref_streaming_method"
internal const val PREF_MIN_PAUSE_THRESHOLD = "pref_playback_resume_rewind_min_pause_threshold"
internal const val PREF_MIN_RESUME_REWIND = "pref_playback_resume_rewind_min"
internal const val PREF_MAX_RESUME_REWIND = "pref_playback_resume_rewind_max"
internal const val PREF_PENDING_RESUME_REWIND = "pref_playback_pending_resume_rewind"
internal const val PENDING_RESUME_REWIND_SEPARATOR = "|"
internal const val PREF_AUTO_REWIND_STOP_AT_CHAPTER = "pref_playback_auto_rewind_stop_at_chapter"
internal const val PREF_AUTO_REWIND_ON_RESUME = "pref_playback_auto_rewind_on_resume"
internal const val PREF_BOOK_TIME_UI = "pref_book_time_playback_ui"
internal const val PREF_WAVY_SLIDER = "pref_wavy_playback_slider"
internal const val PREF_SCROLLING_TITLES = "pref_scrolling_titles"

internal const val PLAYBACK_RATES_SEPARATOR = "::"

internal const val DEFAULT_FORWARD_TIME_MS = 30L * 1000L // 30s
internal const val DEFAULT_BACKWARD_TIME_MS = 10L * 1000L // 15s
internal const val DEFAULT_TRACK_RESET_THRESHOLD_SECONDS = 5.0 // 5s
internal val DEFAULT_PLAYBACK_RATES = listOf(1f, 1.1f, 1.25f, 1.5f, 2f)
internal const val DEFAULT_PLAYBACK_SPEED = 1f
internal const val DEFAULT_REMOTE_NEXT_PREV_SKIPS_CHAPTERS = false
internal const val DEFAULT_AUTO_SYNC = true
internal const val DEFAULT_PLAYBACK_HISTORY = true
internal val DEFAULT_SYNC_INTERVAL_UNMETERED = 15.seconds
internal val DEFAULT_SYNC_INTERVAL_METERED = 60.seconds
internal const val DEFAULT_AUTO_REWIND_ON_RESUME = false
internal const val DEFAULT_AUTO_REWIND_STOP_AT_CHAPTER = true
