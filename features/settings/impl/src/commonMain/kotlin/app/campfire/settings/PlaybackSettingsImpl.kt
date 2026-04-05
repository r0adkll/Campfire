package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.PlaybackSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
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

  private fun String.asFloatList(): List<Float> = split(PLAYBACK_RATES_SEPARATOR).mapNotNull { it.toFloatOrNull() }
}

internal const val PREF_MP3_SEEKING = "pref_playback_mp3_seeking"
internal const val PREF_FORWARD_TIME_MS = "pref_playback_forward_time_ms"
internal const val PREF_BACKWARD_TIME_MS = "pref_playback_backward_time_ms"
internal const val PREF_TRACK_RESET_THRESHOLD = "pref_playback_track_reset_threshold"
internal const val PREF_PLAYBACK_RATES = "pref_playback_rates"
internal const val PREF_PLAYBACK_SPEED = "pref_playback_speed"
internal const val PREF_SYNC = "pref_synchronization"
internal const val PREF_AUTO_SYNC = "pref_auto_sync"

internal const val PLAYBACK_RATES_SEPARATOR = "::"

internal const val DEFAULT_FORWARD_TIME_MS = 30L * 1000L // 30s
internal const val DEFAULT_BACKWARD_TIME_MS = 10L * 1000L // 15s
internal const val DEFAULT_TRACK_RESET_THRESHOLD_SECONDS = 5.0 // 5s
internal val DEFAULT_PLAYBACK_RATES = listOf(1f, 1.1f, 1.25f, 1.5f, 2f)
internal const val DEFAULT_PLAYBACK_SPEED = 1f
internal const val PREF_REMOTE_NEXT_PREV_SKIPS_CHAPTERS = "pref_playback_remote_next_prev_skips_chapters"
internal const val DEFAULT_REMOTE_NEXT_PREV_SKIPS_CHAPTERS = true
internal const val DEFAULT_SYNCHRONIZATION = true
internal const val DEFAULT_AUTO_SYNC = true
internal const val PREF_PLAYBACK_HISTORY = "pref_playback_history_enabled"
internal const val DEFAULT_PLAYBACK_HISTORY = true
