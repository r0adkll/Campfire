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
