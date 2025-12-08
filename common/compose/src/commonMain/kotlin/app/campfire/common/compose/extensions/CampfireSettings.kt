package app.campfire.common.compose.extensions

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.ThemeMode

@Composable
fun CampfireSettings.shouldUseDarkColors(): Boolean {
  val themePreference = remember { observeTheme() }.collectAsState(initial = themeMode)
  return when (themePreference.value) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    else -> isSystemInDarkTheme()
  }
}
