package app.campfire.ui.theming.ui.ai

import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.common.compose.theme.ColorPalette
import app.campfire.core.di.UserScope
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.HalogenResolveResult
import app.campfire.ui.theming.api.HalogenStyle
import app.campfire.ui.theming.api.HalogenThemeManager
import app.campfire.ui.theming.api.screen.AiThemeBuilderScreen
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(AiThemeBuilderScreen::class, UserScope::class)
@Inject
class AiThemeBuilderPresenter(
  private val themeRepository: AppThemeRepository,
  private val halogenThemeManager: HalogenThemeManager,
  @Assisted private val screen: AiThemeBuilderScreen,
  @Assisted private val navigator: Navigator,
) : Presenter<AiThemeBuilderUiState> {

  @Composable
  override fun present(): AiThemeBuilderUiState {
    val coroutineScope = rememberCoroutineScope()

    val prompt = rememberTextFieldState(screen.prompt ?: "")
    val name = rememberTextFieldState(screen.themeName ?: "")

    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var theme by remember { mutableStateOf<AppTheme.Fixed.Ai?>(null) }
    var icon by remember { mutableStateOf(AppTheme.Icon.entries.random()) }
    var style by remember { mutableStateOf(screen.style) }

    return AiThemeBuilderUiState(
      prompt = prompt,
      name = name,
      style = style,
      isGenerating = isGenerating,
      errorMessage = errorMessage,
      theme = theme,
      icon = icon,
    ) { event ->
      when (event) {
        AiThemeBuilderUiEvent.Back -> navigator.pop()

        AiThemeBuilderUiEvent.DismissError -> {
          errorMessage = null
        }

        AiThemeBuilderUiEvent.Clear -> {
          errorMessage = null
          isGenerating = false
          theme = null
          name.clearText()
        }

        AiThemeBuilderUiEvent.Generate -> {
          val promptText = prompt.text.toString().trim()
          if (promptText.isEmpty()) {
            errorMessage = "Describe the theme you want first."
            return@AiThemeBuilderUiState
          }
          coroutineScope.launch {
            isGenerating = true
            errorMessage = null
            val result = halogenThemeManager.resolve(
              key = promptText.hashedKey(),
              prompt = promptText,
              style = style,
            )
            isGenerating = false
            when (result) {
              is HalogenResolveResult.Success -> {
                if (name.text.isEmpty()) {
                  name.setTextAndPlaceCursorAtEnd(promptText)
                }
                theme = result.toCustomTheme(
                  currentName = name.text.toString(),
                  icon = icon,
                  prompt = promptText,
                  style = style,
                )
              }
              is HalogenResolveResult.Failed -> {
                errorMessage = result.reason
              }
            }
          }
        }

        AiThemeBuilderUiEvent.Save -> {
          val current = theme ?: return@AiThemeBuilderUiState
          val themeToSave = current.copy(
            id = screen.id ?: Uuid.random().toHexDashString(),
            name = name.text.toString(),
            icon = icon,
          )
          coroutineScope.launch {
            themeRepository.saveCustomTheme(themeToSave)
            navigator.pop()
          }
        }

        is AiThemeBuilderUiEvent.IconPicked -> {
          icon = event.icon
        }

        is AiThemeBuilderUiEvent.StylePicked -> {
          style = event.style
        }
      }
    }
  }
}

private fun HalogenResolveResult.Success.toCustomTheme(
  currentName: String,
  icon: AppTheme.Icon,
  prompt: String,
  style: HalogenStyle,
): AppTheme.Fixed.Ai = AppTheme.Fixed.Ai(
  id = "ai-pending",
  name = currentName,
  icon = icon,
  prompt = prompt,
  style = style,
  colorPalette = ColorPalette(
    lightColorScheme = palette.lightColorScheme,
    darkColorScheme = palette.darkColorScheme,
  ),
)

/**
 * Stable cache key derived from the prompt text. Same prompt → same cached theme; the
 * user can tweak wording to force a fresh generation.
 */
private fun String.hashedKey(): String = "ai-${this.lowercase().hashCode()}"
