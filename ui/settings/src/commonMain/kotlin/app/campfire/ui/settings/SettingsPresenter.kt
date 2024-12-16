package app.campfire.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.account.api.ServerRepository
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.UserScope
import app.campfire.core.model.Tent
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(SettingsScreen::class, UserScope::class)
@Inject
class SettingsPresenter(
  @Assisted private val navigator: Navigator,
  private val applicationInfo: ApplicationInfo,
  private val settings: CampfireSettings,
  private val serverRepository: ServerRepository,
) : Presenter<SettingsUiState> {

  @Composable
  override fun present(): SettingsUiState {
    val scope = rememberCoroutineScope()

    val tent by remember {
      serverRepository.observeCurrentServer()
        .map { it.tent }
    }.collectAsState(Tent.Default)

    val theme by settings.observeTheme().collectAsState(settings.theme)
    val useDynamicColors by settings.observeUseDynamicColors().collectAsState(settings.useDynamicColors)

    return SettingsUiState(
      tent = tent,
      theme = theme,
      useDynamicColors = useDynamicColors,
      applicationInfo = applicationInfo,
    ) { event ->
      when (event) {
        SettingsUiEvent.Back -> navigator.pop()
        is SettingsUiEvent.Theme -> settings.theme = event.theme
        is SettingsUiEvent.UseDynamicColors -> settings.useDynamicColors = event.useDynamicColors
        is SettingsUiEvent.ChangeTent -> {
          scope.launch {
            serverRepository.changeTent(event.tent)
          }
        }
      }
    }
  }
}
