package app.campfire.common.settings

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = PlaybackSettings::class)
@Inject
class PlaybackSettingsImpl(
  override val settings: ObservableSettings,
  private val dispatcherProvider: DispatcherProvider,
) : PlaybackSettings, AppSettings() {
  private val settingsScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
  private val flowSettings by lazy { settings.toFlowSettings(dispatcherProvider.io) }

  override var enableMp3IndexSeeking: Boolean by booleanSetting(PREF_MP3_SEEKING)
  override fun observeMp3IndexSeeking(): StateFlow<Boolean> {
    return flowSettings.getBooleanFlow(PREF_MP3_SEEKING, false)
      .stateIn(settingsScope, SharingStarted.Lazily, enableMp3IndexSeeking)
  }

  override var forwardTimeMs: Long by longSetting(PREF_FORWARD_TIME_MS, DEFAULT_FORWARD_TIME_MS)
  override var backwardTimeMs: Long by longSetting(PREF_BACKWARD_TIME_MS, DEFAULT_BACKWARD_TIME_MS)

  override fun observeForwardTimeMs(): StateFlow<Long> {
    return flowSettings.getLongFlow(PREF_FORWARD_TIME_MS, DEFAULT_FORWARD_TIME_MS)
      .stateIn(settingsScope, SharingStarted.Lazily, forwardTimeMs)
  }

  override fun observeBackwardTimeMs(): StateFlow<Long> {
    return flowSettings.getLongFlow(PREF_BACKWARD_TIME_MS, DEFAULT_BACKWARD_TIME_MS)
      .stateIn(settingsScope, SharingStarted.Lazily, backwardTimeMs)
  }

  override var trackResetThreshold: Duration by durationSetting(
    key = PREF_TRACK_RESET_THRESHOLD,
    defaultValue = DEFAULT_TRACK_RESET_THRESHOLD_SECONDS.seconds,
  )

  override fun observeTrackResetThreshold(): StateFlow<Duration> {
    return flowSettings.getDoubleFlow(PREF_TRACK_RESET_THRESHOLD, DEFAULT_TRACK_RESET_THRESHOLD_SECONDS)
      .map { it.seconds }
      .stateIn(settingsScope, SharingStarted.Lazily, DEFAULT_TRACK_RESET_THRESHOLD_SECONDS.seconds)
  }

  override var playbackRates: List<Float> by customSetting(
    key = PREF_PLAYBACK_RATES,
    defaultValue = DEFAULT_PLAYBACK_RATES,
    getter = { it.asFloatList() },
    setter = { rates -> rates.joinToString(PLAYBACK_RATES_SEPARATOR) },
  )

  override fun observePlaybackRates(): StateFlow<List<Float>> {
    return flowSettings.getStringOrNullFlow(PREF_PLAYBACK_RATES)
      .mapNotNull { it?.asFloatList() }
      .stateIn(settingsScope, SharingStarted.Lazily, DEFAULT_PLAYBACK_RATES)
  }

  private fun String.asFloatList(): List<Float> = split(PLAYBACK_RATES_SEPARATOR).mapNotNull { it.toFloatOrNull() }
}

internal const val PREF_MP3_SEEKING = "pref_playback_mp3_seeking"
internal const val PREF_FORWARD_TIME_MS = "pref_playback_forward_time_ms"
internal const val PREF_BACKWARD_TIME_MS = "pref_playback_backward_time_ms"
internal const val PREF_TRACK_RESET_THRESHOLD = "pref_playback_track_reset_threshold"
internal const val PREF_PLAYBACK_RATES = "pref_playback_rates"

internal const val PLAYBACK_RATES_SEPARATOR = "::"

internal const val DEFAULT_FORWARD_TIME_MS = 15L * 1000L // 30s
internal const val DEFAULT_BACKWARD_TIME_MS = 10L * 1000L // 15s
internal const val DEFAULT_TRACK_RESET_THRESHOLD_SECONDS = 5.0 // 5s
internal val DEFAULT_PLAYBACK_RATES = listOf(0.5f, 1f, 1.5f, 2f, 3f)
