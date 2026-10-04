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
import app.campfire.settings.store.SettingsDispatcher
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<PlaybackSettings>())
@Inject
class PlaybackSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
  @SettingsDispatcher override val dispatcher: CoroutineDispatcher,
) : PlaybackSettings, AppSettings() {

  private val enableMp3IndexSeekingProperty =
    booleanSetting(PREF_MP3_SEEKING, PlaybackSettings.DEFAULT_MP3_INDEX_SEEKING)
  override fun setEnableMp3IndexSeeking(value: Boolean) = enableMp3IndexSeekingProperty.set(value)
  override fun observeMp3IndexSeeking(): Flow<Boolean> = enableMp3IndexSeekingProperty.observe()

  private val forwardTimeMsProperty =
    longSetting(PREF_FORWARD_TIME_MS, PlaybackSettings.DefaultForwardTime.inWholeMilliseconds)
  override fun setForwardTimeMs(value: Long) = forwardTimeMsProperty.set(value)
  override fun observeForwardTimeMs(): Flow<Long> = forwardTimeMsProperty.observe()

  private val backwardTimeMsProperty =
    longSetting(PREF_BACKWARD_TIME_MS, PlaybackSettings.DefaultBackwardTime.inWholeMilliseconds)
  override fun setBackwardTimeMs(value: Long) = backwardTimeMsProperty.set(value)
  override fun observeBackwardTimeMs(): Flow<Long> = backwardTimeMsProperty.observe()

  private val trackResetThresholdProperty = durationSetting(
    key = PREF_TRACK_RESET_THRESHOLD,
    defaultValue = PlaybackSettings.DefaultTrackResetThreshold,
  )
  override fun setTrackResetThreshold(value: Duration) = trackResetThresholdProperty.set(value)
  override fun observeTrackResetThreshold(): Flow<Duration> = trackResetThresholdProperty.observe()

  private val playbackRatesProperty = customSetting(
    key = PREF_PLAYBACK_RATES,
    defaultValue = PlaybackSettings.DefaultPlaybackRates,
    getter = { it.asFloatList() },
    setter = { rates -> rates.joinToString(PLAYBACK_RATES_SEPARATOR) },
  )
  override fun setPlaybackRates(value: List<Float>) = playbackRatesProperty.set(value)
  override fun observePlaybackRates(): Flow<List<Float>> = playbackRatesProperty.observe()

  private val playbackSpeedProperty = floatSetting(PREF_PLAYBACK_SPEED, PlaybackSettings.DEFAULT_PLAYBACK_SPEED)
  override fun setPlaybackSpeed(value: Float) = playbackSpeedProperty.set(value)
  override fun observePlaybackSpeed(): Flow<Float> = playbackSpeedProperty.observe()

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
  override fun setItemPlaybackSpeeds(value: Map<LibraryItemId, Float>) = itemPlaybackSpeedsProperty.set(value)
  override fun observeItemPlaybackSpeeds(): Flow<Map<LibraryItemId, Float>> =
    itemPlaybackSpeedsProperty.observe()

  override fun setPlaybackSpeedFor(itemId: LibraryItemId?, speed: Float) = edit {
    val speeds = itemPlaybackSpeedsProperty.readInEdit()
    if (itemId != null && itemId in speeds) {
      itemPlaybackSpeedsProperty.writeInEdit(speeds + (itemId to speed))
    } else {
      playbackSpeedProperty.writeInEdit(speed)
    }
  }

  private val remoteNextPrevSkipsChaptersProperty = booleanSetting(
    PREF_REMOTE_NEXT_PREV_SKIPS_CHAPTERS,
    PlaybackSettings.DEFAULT_REMOTE_NEXT_PREV_SKIPS_CHAPTERS,
  )
  override fun setRemoteNextPrevSkipsChapters(value: Boolean) = remoteNextPrevSkipsChaptersProperty.set(value)
  override fun observeRemoteNextPrevSkipsChapters(): Flow<Boolean> = remoteNextPrevSkipsChaptersProperty.observe()

  private val syncEnabledProperty = booleanSetting(PREF_SYNC, PlaybackSettings.DEFAULT_SYNC_ENABLED)
  override fun setSyncEnabled(value: Boolean) = syncEnabledProperty.set(value)
  override fun observeSyncEnabled(): Flow<Boolean> = syncEnabledProperty.observe()

  private val autoSyncEnabledProperty = booleanSetting(PREF_AUTO_SYNC, DEFAULT_AUTO_SYNC)
  override fun setAutoSyncEnabled(value: Boolean) = autoSyncEnabledProperty.set(value)
  override fun observeAutoSyncEnabled(): Flow<Boolean> = autoSyncEnabledProperty.observe()

  private val playbackHistoryEnabledProperty =
    booleanSetting(PREF_PLAYBACK_HISTORY, PlaybackSettings.DEFAULT_PLAYBACK_HISTORY_ENABLED)
  override fun setPlaybackHistoryEnabled(value: Boolean) = playbackHistoryEnabledProperty.set(value)
  override fun observePlaybackHistoryEnabled(): Flow<Boolean> = playbackHistoryEnabledProperty.observe()

  private val syncIntervalUnmeteredProperty =
    durationSetting(PREF_SYNC_INTERVAL_UNMETERED, DEFAULT_SYNC_INTERVAL_UNMETERED)
  override fun setSyncIntervalUnmetered(value: Duration) = syncIntervalUnmeteredProperty.set(value)
  override fun observeSyncIntervalUnmetered(): Flow<Duration> = syncIntervalUnmeteredProperty.observe()

  private val syncIntervalMeteredProperty = durationSetting(PREF_SYNC_INTERVAL_METERED, DEFAULT_SYNC_INTERVAL_METERED)
  override fun setSyncIntervalMetered(value: Duration) = syncIntervalMeteredProperty.set(value)
  override fun observeSyncIntervalMetered(): Flow<Duration> = syncIntervalMeteredProperty.observe()

  private val streamingMethodProperty = enumSetting(PREF_STREAMING_METHOD, StreamingMethod)
  override fun setStreamingMethod(value: StreamingMethod) = streamingMethodProperty.set(value)
  override fun observeStreamingMethod(): Flow<StreamingMethod> = streamingMethodProperty.observe()

  private val autoRewindOnResumeEnabledProperty = booleanSetting(
    PREF_AUTO_REWIND_ON_RESUME,
    DEFAULT_AUTO_REWIND_ON_RESUME,
  )
  override fun setAutoRewindOnResumeEnabled(value: Boolean) = autoRewindOnResumeEnabledProperty.set(value)
  override fun observeAutoRewindOnResumeEnabled(): Flow<Boolean> = autoRewindOnResumeEnabledProperty.observe()

  private val minPauseThresholdProperty = durationSetting(
    PREF_MIN_PAUSE_THRESHOLD,
    ResumeRewindConfig.Default.minPauseThreshold,
  )

  private val minResumeRewindProperty = durationSetting(PREF_MIN_RESUME_REWIND, ResumeRewindConfig.Default.minRewind)

  private val maxResumeRewindProperty = durationSetting(PREF_MAX_RESUME_REWIND, ResumeRewindConfig.Default.maxRewind)

  override fun setResumeRewindConfig(value: ResumeRewindConfig) = edit {
    minPauseThresholdProperty.writeInEdit(value.minPauseThreshold)
    minResumeRewindProperty.writeInEdit(value.minRewind)
    maxResumeRewindProperty.writeInEdit(value.maxRewind)
  }

  override fun observeResumeRewindConfig(): Flow<ResumeRewindConfig> = combine(
    minPauseThresholdProperty.observe(),
    minResumeRewindProperty.observe(),
    maxResumeRewindProperty.observe(),
  ) { minPause, minRewind, maxRewind ->
    ResumeRewindConfig(minPause, minRewind, maxRewind)
  }

  private val autoRewindStopAtChapterBoundaryProperty = booleanSetting(
    PREF_AUTO_REWIND_STOP_AT_CHAPTER,
    DEFAULT_AUTO_REWIND_STOP_AT_CHAPTER,
  )
  override fun setAutoRewindStopAtChapterBoundary(value: Boolean) = autoRewindStopAtChapterBoundaryProperty.set(value)
  override fun observeAutoRewindStopAtChapterBoundary(): Flow<Boolean> =
    autoRewindStopAtChapterBoundaryProperty.observe()

  private val pendingResumeRewindProperty = setting(
    key = PREF_PENDING_RESUME_REWIND,
    read = { getStringOrNull(PREF_PENDING_RESUME_REWIND)?.toPendingResumeRewind() },
    write = {
      if (it == null) remove(PREF_PENDING_RESUME_REWIND) else putString(PREF_PENDING_RESUME_REWIND, it.serialize())
    },
  )
  override fun setPendingResumeRewind(value: PendingResumeRewind?) = pendingResumeRewindProperty.set(value)
  override fun observePendingResumeRewind(): Flow<PendingResumeRewind?> = pendingResumeRewindProperty.observe()

  private val bookTimeInPlaybackUiProperty = booleanSetting(
    PREF_BOOK_TIME_UI,
    PlaybackSettings.DEFAULT_BOOK_TIME_IN_PLAYBACK_UI,
  )
  override fun setBookTimeInPlaybackUi(value: Boolean) = bookTimeInPlaybackUiProperty.set(value)
  override fun observeBookTimeInPlaybackUi(): Flow<Boolean> =
    bookTimeInPlaybackUiProperty.observe()

  private val playbackWavyScrubberProperty = booleanSetting(
    PREF_WAVY_SLIDER,
    PlaybackSettings.DEFAULT_PLAYBACK_WAVY_SCRUBBER,
  )
  override fun setPlaybackWavyScrubber(value: Boolean) = playbackWavyScrubberProperty.set(value)
  override fun observePlaybackWavyScrubber(): Flow<Boolean> =
    playbackWavyScrubberProperty.observe()

  private val scrollingTitlesProperty = booleanSetting(
    PREF_SCROLLING_TITLES,
    PlaybackSettings.DEFAULT_SCROLLING_TITLES,
  )
  override fun setScrollingTitles(value: Boolean) = scrollingTitlesProperty.set(value)
  override fun observeScrollingTitles(): Flow<Boolean> =
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

internal const val DEFAULT_AUTO_SYNC = true
internal val DEFAULT_SYNC_INTERVAL_UNMETERED = 15.seconds
internal val DEFAULT_SYNC_INTERVAL_METERED = 60.seconds
internal const val DEFAULT_AUTO_REWIND_ON_RESUME = false
internal const val DEFAULT_AUTO_REWIND_STOP_AT_CHAPTER = true
