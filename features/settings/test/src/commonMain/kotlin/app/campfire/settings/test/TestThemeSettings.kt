package app.campfire.settings.test

import app.campfire.settings.api.ThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

class TestThemeSettings(
  private val testScope: CoroutineScope = TestScope(StandardTestDispatcher()),
) : TestSettings(), ThemeSettings {

  override var dynamicallyThemeItemDetail: Boolean by boolean()
  override fun observeDynamicallyThemeItemDetail(): StateFlow<Boolean> =
    observeBoolean(::dynamicallyThemeItemDetail)
      .stateIn(testScope, SharingStarted.Lazily, dynamicallyThemePlayback)

  override var dynamicallyThemePlayback: Boolean by boolean()
  override fun observeDynamicallyThemePlayback(): StateFlow<Boolean> =
    observeBoolean(::dynamicallyThemePlayback)
      .stateIn(testScope, SharingStarted.Lazily, dynamicallyThemeItemDetail)
}
