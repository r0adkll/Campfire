// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider

enum class ThemeMode(override val storageKey: String) : EnumSetting {
  LIGHT("light"),
  DARK("dark"),
  SYSTEM("system"),
  ;

  companion object : EnumSettingProvider<ThemeMode> {
    override fun fromStorageKey(key: String?): ThemeMode {
      return values().find { it.storageKey == key } ?: SYSTEM
    }
  }
}
