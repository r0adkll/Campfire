// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.UserThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [UserThemeSettings] fake backed by a [MutableStateFlow] for use in tests.
 */
class TestUserThemeSettings : UserThemeSettings {

  private val _themeId = MutableStateFlow<ThemeKey>(ThemeKey.Tent)
  val themeId: ThemeKey get() = _themeId.value
  override fun setThemeId(value: ThemeKey) {
    _themeId.value = value
  }
  override fun observeThemeId(): StateFlow<ThemeKey> = _themeId.asStateFlow()
}
