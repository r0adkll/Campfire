package app.campfire.ui.theming.ui.builder

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import app.campfire.ui.theming.api.AppTheme
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class ThemeBuilderUiState(
  val theme: AppTheme.Fixed.Custom,

  val name: TextFieldState,
  val seedColor: Color,
  val secondaryColorOverride: Color?,
  val tertiaryColorOverride: Color?,
  val errorColorOverride: Color?,
  val neutralColorOverride: Color?,
  val neutralVariantColorOverride: Color?,

  val colorSpec: ColorSpec.SpecVersion,
  val colorStyle: Variant,
  val contrastLevel: ContrastLevel,

  val eventSink: (ThemeBuilderUiEvent) -> Unit,
) : CircuitUiState {

  val isCreatable: Boolean get() = name.text.isNotBlank()
}

sealed interface ThemeBuilderUiEvent : CircuitUiEvent {
  data object Back : ThemeBuilderUiEvent
  data object Save : ThemeBuilderUiEvent
  data object Delete : ThemeBuilderUiEvent

  data class IconPicked(val icon: AppTheme.Icon) : ThemeBuilderUiEvent
  data class SeedColorPicked(val color: Color) : ThemeBuilderUiEvent
  data class SecondaryColorPicked(val color: Color) : ThemeBuilderUiEvent
  data class TertiaryColorPicked(val color: Color) : ThemeBuilderUiEvent
  data class ErrorColorPicked(val color: Color) : ThemeBuilderUiEvent
  data class NeutralColorPicked(val color: Color) : ThemeBuilderUiEvent
  data class NeutralVariantColorPicked(val color: Color) : ThemeBuilderUiEvent

  data class ColorSpecClick(val spec: ColorSpec.SpecVersion) : ThemeBuilderUiEvent
  data class ColorStyleClick(val style: Variant) : ThemeBuilderUiEvent
  data class ContrastLevelClick(val level: ContrastLevel) : ThemeBuilderUiEvent
}
