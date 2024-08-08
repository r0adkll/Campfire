package app.campfire.common.settings

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider
import kotlinx.coroutines.flow.Flow

interface CampfireSettings {

  var theme: Theme
  fun observeTheme(): Flow<Theme>

  var useDynamicColors: Boolean
  fun observeUseDynamicColors(): Flow<Boolean>

  var currentServerUrl: String?
  fun observeCurrentServerUrl(): Flow<String?>

  enum class Theme(override val storageKey: String) : EnumSetting {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system"),
    ;

    companion object : EnumSettingProvider<Theme> {
      override fun fromStorageKey(key: String?): Theme {
        return values().find { it.storageKey == key } ?: SYSTEM
      }
    }
  }
}
