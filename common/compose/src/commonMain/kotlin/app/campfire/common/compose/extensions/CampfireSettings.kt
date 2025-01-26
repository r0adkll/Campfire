package app.campfire.common.compose.extensions

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.campfire.settings.api.CampfireSettings

@Composable
fun CampfireSettings.shouldUseDarkColors(): Boolean {
  val themePreference = remember { observeTheme() }.collectAsState(initial = theme)
  return when (themePreference.value) {
    CampfireSettings.Theme.LIGHT -> false
    CampfireSettings.Theme.DARK -> true
    else -> isSystemInDarkTheme()
  }
}

@Composable
fun CampfireSettings.shouldUseDynamicColors(): Boolean {
  return remember { observeUseDynamicColors() }
    .collectAsState(initial = useDynamicColors)
    .value
}
