package app.campfire.ui.theming.ui.ai

import androidx.compose.foundation.text.input.TextFieldState
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.HalogenStyle
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class AiThemeBuilderUiState(
  val prompt: TextFieldState,
  val name: TextFieldState,
  val style: HalogenStyle,
  val isGenerating: Boolean,
  val errorMessage: String?,
  val theme: AppTheme.Fixed.Ai?,
  val icon: AppTheme.Icon,
  val eventSink: (AiThemeBuilderUiEvent) -> Unit,
) : CircuitUiState {
  val isSavable: Boolean get() = theme != null && name.text.isNotBlank()
}

sealed interface AiThemeBuilderUiEvent : CircuitUiEvent {
  data object Back : AiThemeBuilderUiEvent
  data object Generate : AiThemeBuilderUiEvent
  data object Save : AiThemeBuilderUiEvent
  data object Clear : AiThemeBuilderUiEvent
  data object DismissError : AiThemeBuilderUiEvent
  data class IconPicked(val icon: AppTheme.Icon) : AiThemeBuilderUiEvent
  data class StylePicked(val style: HalogenStyle) : AiThemeBuilderUiEvent
}
