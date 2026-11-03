// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import app.campfire.core.Platform
import app.campfire.core.coroutines.LoadState
import app.campfire.core.currentPlatform
import app.campfire.core.di.UserScope
import app.campfire.settings.api.ThemeMode
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.AiThemeBuilder
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.screen.AiThemeBuilderScreen
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.campfire.ui.theming.ui.preview.PreviewContentSource
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@CircuitInject(ThemePickerScreen::class, UserScope::class)
@Inject
class ThemePickerPresenter(
  private val appThemeRepository: AppThemeRepository,
  private val themeSettings: ThemeSettings,
  private val previewContentSource: PreviewContentSource,
  private val navigator: Navigator,
  // Defaults to NoOp when the optional :ui:theming:ai module isn't in this build.
  private val aiThemeBuilder: AiThemeBuilder,
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

    val customThemes: LoadState<out List<AppTheme.Fixed>> by remember {
      appThemeRepository.observeCustomThemes()
        .map { LoadState.Loaded(it) }
        .catch<LoadState<out List<AppTheme.Fixed>>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    val currentTheme: AppTheme by rememberRetained {
      appThemeRepository.observeCurrentAppTheme()
    }.collectAsState()

    val aiThemeBuilderAvailable: Boolean by aiThemeBuilder.observeIsAvailable()
      .collectAsState()

    val themeMode: ThemeMode by remember { themeSettings.observeTheme() }.collectAsState()

    val previewContent by remember { previewContentSource.observe() }.collectAsState(PreviewContent())

    val scope = rememberCoroutineScope()

    return ThemePickerUiState(
      currentTheme = currentTheme,
      themeMode = themeMode,
      builtInThemes = builtInThemes,
      customThemes = customThemes,
      aiThemeBuilderAvailable = aiThemeBuilderAvailable,
      previewContent = previewContent,
    ) { event ->
      when (event) {
        ThemePickerUiEvent.Back -> navigator.pop()
        is ThemePickerUiEvent.OpenThemeBuilder -> {
          when (val theme = event.theme) {
            // Without the AI module the screen has no presenter; fall back to the
            // regular builder, which loads AI themes via their custom theme id.
            is AppTheme.Fixed.Ai -> if (aiThemeBuilder.isSupported) {
              navigator.goTo(AiThemeBuilderScreen(theme))
            } else {
              navigator.goTo(ThemeBuilderScreen(theme.id))
            }
            is AppTheme.Fixed.Custom -> navigator.goTo(ThemeBuilderScreen(theme.id))
            else -> navigator.goTo(ThemeBuilderScreen())
          }
        }
        ThemePickerUiEvent.OpenAiThemeBuilder -> navigator.goTo(AiThemeBuilderScreen())
        is ThemePickerUiEvent.SelectTheme -> appThemeRepository.setCurrentTheme(event.theme)
        is ThemePickerUiEvent.SetThemeMode -> themeSettings.themeMode = event.mode
        is ThemePickerUiEvent.Customize -> navigator.goTo(
          ThemeBuilderScreen(
            seedColor = event.seedColor.toArgb(),
            icon = (event.theme as? AppTheme.Fixed)?.icon,
          ),
        )
        is ThemePickerUiEvent.Duplicate -> scope.launch {
          appThemeRepository.saveCustomTheme(event.theme.copied(event.name))
        }
        is ThemePickerUiEvent.Delete -> scope.launch {
          appThemeRepository.deleteCustomTheme(event.theme.id)
        }
      }
    }
  }
}

/**
 * A copy of one of the user's themes, under a new id and [name]. Built-in themes aren't copied;
 * they're customized into a new theme instead.
 */
internal fun AppTheme.Fixed.copied(name: String): AppTheme.Fixed {
  val id = Uuid.random().toHexDashString()
  return when (this) {
    is AppTheme.Fixed.Custom -> copy(id = id, name = name)
    is AppTheme.Fixed.Ai -> copy(id = id, name = name)
    else -> error("Only the user's own themes are copied, not $this")
  }
}
