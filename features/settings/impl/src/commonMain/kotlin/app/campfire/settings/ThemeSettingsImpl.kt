// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.ThemeMode
import app.campfire.settings.api.ThemeSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ThemeSettings>())
@Inject
class ThemeSettingsImpl(
  @ForScope(AppScope::class) override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : ThemeSettings, AppSettings() {

  private val dynamicallyThemeItemDetailProperty = booleanSetting(KEY_ITEM_DETAIL_THEMING, true)
  override var dynamicallyThemeItemDetail: Boolean by dynamicallyThemeItemDetailProperty
  override fun observeDynamicallyThemeItemDetail(): StateFlow<Boolean> = dynamicallyThemeItemDetailProperty.observe()

  private val dynamicallyThemePlaybackProperty = booleanSetting(KEY_PLAYBACK_THEMING, true)
  override var dynamicallyThemePlayback: Boolean by dynamicallyThemePlaybackProperty
  override fun observeDynamicallyThemePlayback(): StateFlow<Boolean> = dynamicallyThemePlaybackProperty.observe()

  override var themeId: ThemeKey by customSetting(
    key = KEY_CURRENT_THEME,
    defaultValue = ThemeKey.Tent,
    getter = { ThemeKey.from(it) },
    setter = { it.storageKey },
  )

  private val themeModeProperty = enumSetting(KEY_THEME, ThemeMode)
  override var themeMode: ThemeMode by themeModeProperty
  override fun observeTheme(): StateFlow<ThemeMode> = themeModeProperty.observe()
}

internal const val KEY_ITEM_DETAIL_THEMING = "pref_dynamically_theme_item_detail"
internal const val KEY_PLAYBACK_THEMING = "pref_dynamically_theme_playback"
internal const val KEY_CURRENT_THEME = "pref_current_theme"
internal const val KEY_THEME = "pref_theme"
internal const val KEY_USE_DYNAMIC_COLORS = "pref_dynamic_colors"
