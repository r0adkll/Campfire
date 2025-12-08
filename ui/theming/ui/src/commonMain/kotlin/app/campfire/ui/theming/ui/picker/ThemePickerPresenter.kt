package app.campfire.ui.theming.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.core.Platform
import app.campfire.core.coroutines.LoadState
import app.campfire.core.currentPlatform
import app.campfire.core.di.UserScope
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(ThemePickerScreen::class, UserScope::class)
@Inject
class ThemePickerPresenter(
  private val appThemeRepository: AppThemeRepository,
  @Assisted private val navigator: Navigator,
) : Presenter<ThemePickerUiState> {

  @Composable
  override fun present(): ThemePickerUiState {
    val builtInThemes = remember {
      buildList {
        add(AppTheme.Fixed.Tent)
        add(AppTheme.Fixed.Forest)
        add(AppTheme.Fixed.WaterBottle)
        add(AppTheme.Fixed.Rucksack)
        add(AppTheme.Fixed.LifeFloat)
        add(AppTheme.Fixed.Mountain)

        if (currentPlatform == Platform.ANDROID) {
          add(AppTheme.Dynamic)
        }
      }
    }

    val customThemes: LoadState<out List<AppTheme.Fixed.Custom>> by remember {
      appThemeRepository.observeCustomThemes()
        .map { LoadState.Loaded(it) }
        .catch<LoadState<out List<AppTheme.Fixed.Custom>>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    val currentTheme: AppTheme by rememberRetained {
      appThemeRepository.observeCurrentAppTheme()
    }.collectAsState()

    return ThemePickerUiState(
      currentTheme = currentTheme,
      builtInThemes = builtInThemes,
      customThemes = customThemes,
    ) { event ->
      when (event) {
        ThemePickerUiEvent.Back -> navigator.pop()
        is ThemePickerUiEvent.OpenThemeBuilder -> navigator.goTo(ThemeBuilderScreen(event.theme?.id))
        is ThemePickerUiEvent.SelectTheme -> appThemeRepository.setCurrentTheme(event.theme)
      }
    }
  }
}
