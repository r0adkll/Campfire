package app.campfire.common.settings

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
}

internal const val PREF_MP3_SEEKING = "pref_playback_mp3_seeking"
internal const val PREF_FORWARD_TIME_MS = "pref_playback_forward_time_ms"
internal const val PREF_BACKWARD_TIME_MS = "pref_playback_backward_time_ms"

internal const val DEFAULT_FORWARD_TIME_MS = 15L * 1000L // 30s
internal const val DEFAULT_BACKWARD_TIME_MS = 10L * 1000L // 15s
