// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.di.UserScope
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import app.campfire.ui.theming.ui.builder.color.specVersion
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.campfire.ui.theming.ui.preview.PreviewContentSource
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch

internal const val NEW_THEME_ID = "new-theme-id"

internal val AppTheme.Fixed.Custom.isNew: Boolean get() = id == NEW_THEME_ID

private val DefaultColorStyle = Variant.EXPRESSIVE

/** The Tent theme's primary color, so a new theme starts close to Campfire's default look. */
private val DefaultSeedColor = Color(0xFF904B3C)

@CircuitInject(ThemeBuilderScreen::class, UserScope::class)
@Inject
class ThemeBuilderPresenter(
  private val themeRepository: AppThemeRepository,
  private val previewContentSource: PreviewContentSource,
  private val screen: ThemeBuilderScreen,
  private val navigator: Navigator,
) : Presenter<ThemeBuilderUiState> {

  @Composable
  override fun present(): ThemeBuilderUiState {
    val scope = rememberCoroutineScope()

    val id by remember { mutableStateOf(screen.customThemeId ?: NEW_THEME_ID) }
    val name = rememberTextFieldState()
    var icon by remember { mutableStateOf(AppTheme.Icon.Tent) }
    var colors by remember { mutableStateOf(CustomThemeColors(DefaultSeedColor, DefaultColorStyle)) }

    val customThemes by remember { themeRepository.observeCustomThemes() }.collectAsState(emptyList())

    LaunchedEffect(Unit) {
      screen.customThemeId?.let { id ->
        themeRepository.getCustomTheme(id)
          .onSuccess { theme ->
            require(theme is AppTheme.Fixed.Custom)
            name.edit { replace(0, length, theme.name) }
            icon = theme.icon
            colors = CustomThemeColors(
              seedColor = theme.seedColor,
              colorStyle = theme.colorStyle,
              colorSpec = theme.colorSpec,
              contrastLevel = theme.contrastLevel.toDouble(),
              secondaryColor = theme.secondaryColorOverride,
              tertiaryColor = theme.tertiaryColorOverride,
              errorColor = theme.errorColorOverride,
              neutralColor = theme.neutralColorOverride,
              neutralVariantColor = theme.neutralVariantColorOverride,
            )
          }
      }
    }

    val colorPalette = remember(colors) { colors.toColorPalette() }
    val contrast = remember(colorPalette) { contrastStatuses(colorPalette) }
    val palettes = remember(colors) { paletteRows(colors) }
    val styles = remember(colors.copy(colorStyle = DefaultColorStyle, colorSpec = DefaultColorStyle.specVersion)) {
      styleOptions(colors)
    }

    val theme = AppTheme.Fixed.Custom(
      id = id,
      name = name.text.toString(),
      icon = icon,
      seedColor = colors.seedColor,
      colorSpec = colors.colorSpec,
      colorStyle = colors.colorStyle,
      contrastLevel = colors.contrastLevel.toFloat(),
      secondaryColorOverride = colors.secondaryColor,
      tertiaryColorOverride = colors.tertiaryColor,
      errorColorOverride = colors.errorColor,
      neutralColorOverride = colors.neutralColor,
      neutralVariantColorOverride = colors.neutralVariantColor,
      colorPalette = colorPalette,
    )

    val previewContent by remember { previewContentSource.observe() }.collectAsState(PreviewContent())

    return ThemeBuilderUiState(
      theme = theme,
      name = name,
      styles = styles,
      palettes = palettes,
      contrast = contrast,
      customThemeCount = customThemes.size,
      previewContent = previewContent,
    ) { event ->
      when (event) {
        ThemeBuilderUiEvent.Back -> navigator.pop()

        ThemeBuilderUiEvent.CoverThemingSettings -> {
          navigator.goTo(SettingsScreen(SettingsScreen.Page.Appearance))
        }

        ThemeBuilderUiEvent.Save -> {
          val themeToSave = if (theme.isNew) theme.copy(id = Uuid.random().toHexDashString()) else theme
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

        is ThemeBuilderUiEvent.IconPicked -> icon = event.icon
        is ThemeBuilderUiEvent.ColorPicked -> colors = colors.withColor(event.key, event.color)
        is ThemeBuilderUiEvent.ColorReset -> colors = colors.withColor(event.key, null)
        is ThemeBuilderUiEvent.StyleClick -> {
          colors = colors.copy(colorStyle = event.style, colorSpec = event.style.specVersion)
        }
        is ThemeBuilderUiEvent.ContrastLevelClick -> {
          colors = colors.copy(contrastLevel = event.level.contrast.toDouble())
        }
      }
    }
  }
}

/**
 * Applies a pick for [key], or clears it when [color] is null. Backgrounds set both neutral
 * palettes, since the 2025 spec only reads one and the 2021 spec splits surfaces across both.
 */
private fun CustomThemeColors.withColor(key: PaletteKey, color: Color?): CustomThemeColors = when (key) {
  PaletteKey.Accent -> if (color != null) copy(seedColor = color) else this
  PaletteKey.Supporting -> copy(secondaryColor = color)
  PaletteKey.ThirdAccent -> copy(tertiaryColor = color)
  PaletteKey.Backgrounds -> copy(neutralColor = color, neutralVariantColor = color)
  PaletteKey.Errors -> copy(errorColor = color)
}
