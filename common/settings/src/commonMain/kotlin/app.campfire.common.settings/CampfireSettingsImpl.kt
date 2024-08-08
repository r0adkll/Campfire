package app.campfire.common.settings

import app.campfire.common.settings.CampfireSettings.Theme
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@Inject
class CampfireSettingsImpl(
  override val settings: ObservableSettings,
  private val dispatchers: app.campfire.core.coroutines.DispatcherProvider,
) : CampfireSettings, AppSettings() {
  private val flowSettings by lazy { settings.toFlowSettings(dispatchers.io) }

  override var theme: Theme by enumSetting(KEY_THEME, Theme)
  override fun observeTheme(): Flow<Theme> {
    return flowSettings.getEnumFlow(KEY_THEME, Theme)
  }

  override var useDynamicColors: Boolean by booleanSetting(KEY_USE_DYNAMIC_COLORS, false)
  override fun observeUseDynamicColors(): Flow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_USE_DYNAMIC_COLORS, false)
  }

  override var currentServerUrl: String? by stringOrNullSetting(KEY_CURRENT_SERVER_URL)
  override fun observeCurrentServerUrl(): Flow<String?> {
    return flowSettings.getStringOrNullFlow(KEY_CURRENT_SERVER_URL)
  }
}

internal const val KEY_THEME = "pref_theme"
internal const val KEY_USE_DYNAMIC_COLORS = "pref_dynamic_colors"
internal const val KEY_CURRENT_SERVER_URL = "pref_current_server_url"
