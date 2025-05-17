package app.campfire.settings

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.settings.api.DevSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<DevSettings>())
@Inject
class DevSettingsImpl(
  override val settings: ObservableSettings,
  private val dispatcherProvider: DispatcherProvider,
) : DevSettings, AppSettings() {
  private val settingsScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
  private val flowSettings by lazy { settings.toFlowSettings(dispatcherProvider.io) }

  private val defaultSessionAge get() = 10.minutes
  override var sessionAge: Duration by durationSetting(KEY_SESSION_AGE, defaultSessionAge)

  override fun observeSessionAge(): StateFlow<Duration> {
    return flowSettings.getDurationFlow(KEY_SESSION_AGE, defaultSessionAge)
      .stateIn(settingsScope, SharingStarted.Lazily, defaultSessionAge)
  }
}

internal const val KEY_SESSION_AGE = "pref_dev_setting_session_age"
