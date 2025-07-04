package app.campfire.settings

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.DevSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = DevSettings::class)
@Inject
class DevSettingsImpl(
  override val settings: ObservableSettings,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(AppScope::class) val coroutineScope: CoroutineScope,
) : DevSettings, AppSettings() {
  private val settingsScope = coroutineScope + dispatcherProvider.io
  private val flowSettings by lazy { settings.toFlowSettings(dispatcherProvider.io) }

  private val defaultDeveloperMode get() = false
  override var developerModeEnabled: Boolean by booleanSetting(KEY_DEVELOPER_MODE, defaultDeveloperMode)

  override fun observeDeveloperMode(): StateFlow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_DEVELOPER_MODE, defaultDeveloperMode)
      .stateIn(settingsScope, SharingStarted.Lazily, defaultDeveloperMode)
  }

  private val defaultSessionAge get() = 10.minutes
  override var sessionAge: Duration by durationSetting(KEY_SESSION_AGE, defaultSessionAge)

  override fun observeSessionAge(): StateFlow<Duration> {
    return flowSettings.getDurationFlow(KEY_SESSION_AGE, defaultSessionAge)
      .stateIn(settingsScope, SharingStarted.Lazily, defaultSessionAge)
  }
}

internal const val KEY_DEVELOPER_MODE = "pref_developer_mode_enabled"
internal const val KEY_SESSION_AGE = "pref_dev_setting_session_age"
