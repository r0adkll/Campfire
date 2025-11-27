package app.campfire.settings

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.ThemeSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = ThemeSettings::class)
@Inject
class ThemeSettingsImpl(
  override val settings: ObservableSettings,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(AppScope::class) val coroutineScope: CoroutineScope,
) : ThemeSettings, AppSettings() {
  private val settingsScope = coroutineScope + dispatcherProvider.io
  private val flowSettings by lazy { settings.toFlowSettings(dispatcherProvider.io) }

  override var dynamicallyThemeItemDetail: Boolean by booleanSetting(KEY_ITEM_DETAIL_THEMING, true)
  override fun observeDynamicallyThemeItemDetail(): StateFlow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_ITEM_DETAIL_THEMING, dynamicallyThemeItemDetail)
      .stateIn(settingsScope, SharingStarted.Lazily, dynamicallyThemeItemDetail)
  }

  override var dynamicallyThemePlayback: Boolean by booleanSetting(KEY_PLAYBACK_THEMING, true)
  override fun observeDynamicallyThemePlayback(): StateFlow<Boolean> {
    return flowSettings.getBooleanFlow(KEY_PLAYBACK_THEMING, dynamicallyThemePlayback)
      .stateIn(settingsScope, SharingStarted.Lazily, dynamicallyThemePlayback)
  }
}

internal const val KEY_ITEM_DETAIL_THEMING = "pref_dynamically_theme_item_detail"
internal const val KEY_PLAYBACK_THEMING = "pref_dynamically_theme_playback"
