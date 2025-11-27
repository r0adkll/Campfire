package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

interface ThemeSettings {

  var dynamicallyThemeItemDetail: Boolean
  fun observeDynamicallyThemeItemDetail(): StateFlow<Boolean>

  var dynamicallyThemePlayback: Boolean
  fun observeDynamicallyThemePlayback(): StateFlow<Boolean>
}
