package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.DevSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = DevSettings::class)
@Inject
class DevSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : DevSettings, AppSettings() {

  private val defaultDeveloperMode get() = false
  private val developerModeProperty = booleanSetting(KEY_DEVELOPER_MODE, defaultDeveloperMode)
  override var developerModeEnabled: Boolean by developerModeProperty

  override fun observeDeveloperMode(): StateFlow<Boolean> = developerModeProperty.observe()

  private val defaultSessionAge get() = 10.minutes
  private val sessionAgeProperty = durationSetting(KEY_SESSION_AGE, defaultSessionAge)
  override var sessionAge: Duration by sessionAgeProperty

  override fun observeSessionAge(): StateFlow<Duration> = sessionAgeProperty.observe()
}

internal const val KEY_DEVELOPER_MODE = "pref_developer_mode_enabled"
internal const val KEY_SESSION_AGE = "pref_dev_setting_session_age"
