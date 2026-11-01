// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import app.campfire.core.di.UserScope
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import app.campfire.ui.theming.ui.builder.color.specVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch

internal val ColorStyles = mapOf(
  SpecVersion.SPEC_2025 to listOf(
    Variant.EXPRESSIVE,
    Variant.VIBRANT,
    Variant.TONAL_SPOT,
    Variant.NEUTRAL,
  ),
  SpecVersion.SPEC_2021 to listOf(
    Variant.RAINBOW,
    Variant.FRUIT_SALAD,
    Variant.MONOCHROME,
    Variant.FIDELITY,
    Variant.CONTENT,
  ),
)

internal const val NEW_THEME_ID = "new-theme-id"

internal val AppTheme.Fixed.Custom.isNew: Boolean get() = id == NEW_THEME_ID

private val DefaultColorStyle = Variant.EXPRESSIVE

@CircuitInject(ThemeBuilderScreen::class, UserScope::class)
@Inject
class ThemeBuilderPresenter(
  private val themeRepository: AppThemeRepository,
  private val screen: ThemeBuilderScreen,
  private val navigator: Navigator,
) : Presenter<ThemeBuilderUiState> {

  @Composable
  override fun present(): ThemeBuilderUiState {
    val scope = rememberCoroutineScope()

    val id by remember { mutableStateOf(screen.customThemeId ?: NEW_THEME_ID) }
    val name = rememberTextFieldState()
    var icon by remember { mutableStateOf(AppTheme.Icon.Tent) }

    var seedColor by remember { mutableStateOf(Color.Red) }
    var secondaryOverride by remember { mutableStateOf<Color?>(null) }
    var tertiaryOverride by remember { mutableStateOf<Color?>(null) }
    var errorOverride by remember { mutableStateOf<Color?>(null) }
    var neutralOverride by remember { mutableStateOf<Color?>(null) }
    var neutralVariantOverride by remember { mutableStateOf<Color?>(null) }

    var colorSpec by remember { mutableStateOf(DefaultColorStyle.specVersion) }
    var colorStyle by remember { mutableStateOf(DefaultColorStyle) }
    var contrastLevel by remember { mutableStateOf(ContrastLevel.Normal) }

    // Load the theme from the repository, if exists, and apply to entire view
    LaunchedEffect(Unit) {
      screen.customThemeId?.let { id ->
        themeRepository.getCustomTheme(id)
          .onSuccess { theme ->
            require(theme is AppTheme.Fixed.Custom)
            name.edit { replace(0, length, theme.name) }
            icon = theme.icon
            seedColor = theme.seedColor
            colorSpec = theme.colorSpec
            colorStyle = theme.colorStyle
            contrastLevel = ContrastLevel.from(theme.contrastLevel)
            secondaryOverride = theme.secondaryColorOverride
            tertiaryOverride = theme.tertiaryColorOverride
            errorOverride = theme.errorColorOverride
            neutralOverride = theme.neutralColorOverride
            neutralVariantOverride = theme.neutralVariantColorOverride
          }
      }
    }

    LaunchedEffect(colorSpec) {
      if (colorSpec == SpecVersion.SPEC_2025 && colorStyle !in ColorStyles[SpecVersion.SPEC_2025]!!) {
        colorStyle = Variant.EXPRESSIVE
      }
    }

    val colors = CustomThemeColors(
      seedColor = seedColor,
      colorStyle = colorStyle,
      colorSpec = colorSpec,
      contrastLevel = contrastLevel.contrast.toDouble(),
      secondaryColor = secondaryOverride,
      tertiaryColor = tertiaryOverride,
      errorColor = errorOverride,
      neutralColor = neutralOverride,
      neutralVariantColor = neutralVariantOverride,
    )
    val colorPalette = remember(colors) { colors.toColorPalette() }

    val theme = AppTheme.Fixed.Custom(
      id = id,
      name = name.text.toString(),
      icon = icon,
      seedColor = seedColor,
      colorSpec = colorSpec,
      colorStyle = colorStyle,
      contrastLevel = contrastLevel.contrast,
      secondaryColorOverride = secondaryOverride,
      tertiaryColorOverride = tertiaryOverride,
      errorColorOverride = errorOverride,
      neutralColorOverride = neutralOverride,
      neutralVariantColorOverride = neutralVariantOverride,
      colorPalette = colorPalette,
    )

    return ThemeBuilderUiState(
      theme = theme,
      name = name,
      seedColor = seedColor,
      secondaryColorOverride = secondaryOverride,
      tertiaryColorOverride = tertiaryOverride,
      errorColorOverride = errorOverride,
      neutralColorOverride = neutralOverride,
      neutralVariantColorOverride = neutralVariantOverride,
      colorSpec = colorSpec,
      colorStyle = colorStyle,
      contrastLevel = contrastLevel,
    ) { event ->
      when (event) {
        ThemeBuilderUiEvent.Back -> navigator.pop()

        ThemeBuilderUiEvent.Save -> {
          val themeToSave = if (theme.id == NEW_THEME_ID) {
            theme.copy(id = Uuid.random().toHexDashString())
          } else {
            theme
          }

          scope.launch {
            themeRepository.saveCustomTheme(themeToSave)
            navigator.pop()
          }
        }

        ThemeBuilderUiEvent.Delete -> {
          if (!theme.isNew) {
            scope.launch {
              themeRepository.deleteCustomTheme(theme.id)
              navigator.pop()
            }
          }
        }

        is ThemeBuilderUiEvent.SeedColorPicked -> {
          seedColor = event.color
          secondaryOverride = null
          tertiaryOverride = null
          errorOverride = null
          neutralOverride = null
          neutralVariantOverride = null
        }

        is ThemeBuilderUiEvent.IconPicked -> icon = event.icon

        is ThemeBuilderUiEvent.SecondaryColorPicked -> secondaryOverride = event.color
        is ThemeBuilderUiEvent.TertiaryColorPicked -> tertiaryOverride = event.color
        is ThemeBuilderUiEvent.ErrorColorPicked -> errorOverride = event.color
        is ThemeBuilderUiEvent.NeutralColorPicked -> neutralOverride = event.color
        is ThemeBuilderUiEvent.NeutralVariantColorPicked -> neutralVariantOverride = event.color

        is ThemeBuilderUiEvent.ColorSpecClick -> colorSpec = event.spec
        is ThemeBuilderUiEvent.ColorStyleClick -> colorStyle = event.style
        is ThemeBuilderUiEvent.ContrastLevelClick -> contrastLevel = event.level
      }
    }
  }
}
