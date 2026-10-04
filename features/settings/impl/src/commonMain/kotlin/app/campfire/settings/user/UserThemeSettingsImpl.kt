// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.user

import app.campfire.core.di.UserScope
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.UserThemeSettings
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class, binding = binding<UserThemeSettings>())
@Inject
class UserThemeSettingsImpl(
  @UserSettings override val store: SettingsStore,
) : UserThemeSettings, AppSettings() {

  private val themeIdProperty = customSetting(
    key = KEY_CURRENT_THEME,
    defaultValue = ThemeKey.Tent,
    getter = { ThemeKey.from(it) },
    setter = { it.storageKey },
  )
  override fun setThemeId(value: ThemeKey) = themeIdProperty.set(value)
  override fun observeThemeId(): Flow<ThemeKey> = themeIdProperty.observe()
}

internal const val KEY_CURRENT_THEME = "pref_current_theme"
