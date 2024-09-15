package app.campfire.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(SettingsScreen::class, UserScope::class)
@Inject
class SettingsPresenter(
  @Assisted private val navigator: Navigator,
  private val applicationInfo: ApplicationInfo,
  private val settings: CampfireSettings,
) : Presenter<SettingsUiState> {

  @Composable
  override fun present(): SettingsUiState {
    val theme by settings.observeTheme().collectAsState(settings.theme)
    val useDynamicColors by settings.observeUseDynamicColors().collectAsState(settings.useDynamicColors)

    return SettingsUiState(
      theme = theme,
      useDynamicColors = useDynamicColors,
      applicationInfo = applicationInfo,
    ) { event ->
      when (event) {
        SettingsUiEvent.Back -> navigator.pop()
        is SettingsUiEvent.Theme -> settings.theme = event.theme
        is SettingsUiEvent.UseDynamicColors -> settings.useDynamicColors = event.useDynamicColors
      }
    }
  }
}
