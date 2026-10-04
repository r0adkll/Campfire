// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.theme

import app.campfire.core.di.AppScope
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.ThemeMode
import app.campfire.settings.api.ThemeSettings
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsStore
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ThemeSettings>())
@Inject
class ThemeSettingsImpl(
  override val store: SettingsStore,
  private val legacySettings: ObservableSettings,
) : ThemeSettings, AppSettings() {

  private val dynamicallyThemeItemDetailProperty =
    booleanSetting(KEY_ITEM_DETAIL_THEMING, ThemeSettings.DEFAULT_DYNAMICALLY_THEME_ITEM_DETAIL)
  override fun setDynamicallyThemeItemDetail(value: Boolean) = dynamicallyThemeItemDetailProperty.set(value)
  override fun observeDynamicallyThemeItemDetail(): Flow<Boolean> = dynamicallyThemeItemDetailProperty.observe()

  private val dynamicallyThemePlaybackProperty =
    booleanSetting(KEY_PLAYBACK_THEMING, ThemeSettings.DEFAULT_DYNAMICALLY_THEME_PLAYBACK)
  override fun setDynamicallyThemePlayback(value: Boolean) = dynamicallyThemePlaybackProperty.set(value)
  override fun observeDynamicallyThemePlayback(): Flow<Boolean> = dynamicallyThemePlaybackProperty.observe()

  private val themeIdProperty = customSetting(
    key = KEY_CURRENT_THEME,
    defaultValue = ThemeKey.Tent,
    getter = { ThemeKey.from(it) },
    setter = { it.storageKey },
  )
  override fun setThemeId(value: ThemeKey) = themeIdProperty.set(value)
  override fun observeThemeId(): Flow<ThemeKey> = themeIdProperty.observe()

  private val themeModeProperty = enumSetting(KEY_THEME, ThemeMode)
  override fun setThemeMode(value: ThemeMode) {
    themeModeProperty.set(value)
    // Mirrored where it can be read without waiting, for lastThemeMode()
    legacySettings.putString(KEY_THEME, value.storageKey)
  }
  override fun observeTheme(): Flow<ThemeMode> = themeModeProperty.observe()

  // The platform preferences can be read on the caller's thread, which DataStore can't; this runs once, to
  // draw the first frame. Keep KEY_THEME there when the other legacy values are removed.
  override fun lastThemeMode(): ThemeMode = ThemeMode.fromStorageKey(legacySettings.getStringOrNull(KEY_THEME))
}

internal const val KEY_ITEM_DETAIL_THEMING = "pref_dynamically_theme_item_detail"
internal const val KEY_PLAYBACK_THEMING = "pref_dynamically_theme_playback"
internal const val KEY_CURRENT_THEME = "pref_current_theme"
internal const val KEY_THEME = "pref_theme"
internal const val KEY_USE_DYNAMIC_COLORS = "pref_dynamic_colors"
