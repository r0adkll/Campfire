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
import app.campfire.common.compose.extensions.asHct
import app.campfire.common.compose.theme.ColorPalette
import app.campfire.core.di.UserScope
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpecs
import com.r0adkll.swatchbuckler.color.dynamiccolor.DynamicScheme
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.r0adkll.swatchbuckler.color.hct.Hct
import com.r0adkll.swatchbuckler.color.palettes.TonalPalette
import com.r0adkll.swatchbuckler.compose.util.asColorScheme
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

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

@CircuitInject(ThemeBuilderScreen::class, UserScope::class)
@Inject
class ThemeBuilderPresenter(
  private val themeRepository: AppThemeRepository,
  @Assisted private val screen: ThemeBuilderScreen,
  @Assisted private val navigator: Navigator,
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

    var colorSpec by remember { mutableStateOf(SpecVersion.SPEC_2021) }
    var colorStyle by remember { mutableStateOf(Variant.EXPRESSIVE) }
    var contrastLevel by remember { mutableStateOf(ContrastLevel.Normal) }

    // Load the theme from the repository, if exists, and apply to entire view
    LaunchedEffect(Unit) {
      screen.customThemeId?.let { id ->
        themeRepository.getCustomTheme(id)
          .onSuccess { theme ->
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

    val theme = buildTheme(
      id = id,
      name = name.text.toString(),
      icon = icon,
      seedColor = seedColor,
      colorSpec = colorSpec,
      colorStyle = colorStyle,
      contrastLevel = contrastLevel,
      secondaryColorOverride = secondaryOverride,
      tertiaryColorOverride = tertiaryOverride,
      errorColorOverride = errorOverride,
      neutralColorOverride = neutralOverride,
      neutralVariantColorOverride = neutralVariantOverride,
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
        is ThemeBuilderUiEvent.NeutralVariantColorPicked -> event.color

        is ThemeBuilderUiEvent.ColorSpecClick -> colorSpec = event.spec
        is ThemeBuilderUiEvent.ColorStyleClick -> colorStyle = event.style
        is ThemeBuilderUiEvent.ContrastLevelClick -> contrastLevel = event.level
      }
    }
  }

  @Composable
  fun buildTheme(
    id: String,
    name: String,
    icon: AppTheme.Icon,
    seedColor: Color,
    colorSpec: SpecVersion,
    colorStyle: Variant,
    contrastLevel: ContrastLevel,
    secondaryColorOverride: Color?,
    tertiaryColorOverride: Color?,
    errorColorOverride: Color?,
    neutralColorOverride: Color?,
    neutralVariantColorOverride: Color?,
  ): AppTheme.Fixed.Custom {
    val contrastLevelDbl = contrastLevel.contrast.toDouble()
    val lightTheme = createScheme(
      sourceColor = seedColor.asHct(),
      secondaryColorOverride = secondaryColorOverride?.asHct(),
      tertiaryColorOverride = tertiaryColorOverride?.asHct(),
      errorColorOverride = errorColorOverride?.asHct(),
      neutralColorOverride = neutralColorOverride?.asHct(),
      neutralVariantColorOverride = neutralVariantColorOverride?.asHct(),
      colorSpec = colorSpec,
      colorStyle = colorStyle,
      contrastLevelDbl = contrastLevelDbl,
      isDark = false,
    )

    val darkTheme = createScheme(
      sourceColor = seedColor.asHct(),
      secondaryColorOverride = secondaryColorOverride?.asHct(),
      tertiaryColorOverride = tertiaryColorOverride?.asHct(),
      errorColorOverride = errorColorOverride?.asHct(),
      neutralColorOverride = neutralColorOverride?.asHct(),
      neutralVariantColorOverride = neutralVariantColorOverride?.asHct(),
      colorSpec = colorSpec,
      colorStyle = colorStyle,
      contrastLevelDbl = contrastLevelDbl,
      isDark = true,
    )

    return AppTheme.Fixed.Custom(
      id = id,
      name = name,
      icon = icon,
      seedColor = seedColor,
      colorSpec = colorSpec,
      colorStyle = colorStyle,
      contrastLevel = contrastLevel.contrast,
      secondaryColorOverride = secondaryColorOverride,
      tertiaryColorOverride = tertiaryColorOverride,
      errorColorOverride = errorColorOverride,
      neutralColorOverride = neutralColorOverride,
      neutralVariantColorOverride = neutralVariantColorOverride,
      colorPalette = ColorPalette(
        lightColorScheme = lightTheme.asColorScheme(),
        darkColorScheme = darkTheme.asColorScheme(),
      ),
    )
  }
}

private fun createScheme(
  sourceColor: Hct,
  secondaryColorOverride: Hct?,
  tertiaryColorOverride: Hct?,
  errorColorOverride: Hct?,
  neutralColorOverride: Hct?,
  neutralVariantColorOverride: Hct?,

  colorSpec: SpecVersion,
  colorStyle: Variant,
  contrastLevelDbl: Double,
  isDark: Boolean,
): DynamicScheme {
  return DynamicScheme(
    sourceColorHct = sourceColor,
    variant = colorStyle,
    isDark = isDark,
    contrastLevel = contrastLevelDbl,
    platform = DynamicScheme.Platform.PHONE,
    specVersion = colorSpec,
    primaryPalette = ColorSpecs
      .get(colorSpec)
      .getPrimaryPalette(
        variant = colorStyle,
        sourceColorHct = sourceColor,
        isDark = isDark,
        platform = DynamicScheme.Platform.PHONE,
        contrastLevel = contrastLevelDbl,
      ),
    secondaryPalette = ColorSpecs
      .get(colorSpec)
      .getOverrideOr(
        overrideColor = secondaryColorOverride,
        colorStyle = colorStyle,
        isDark = isDark,
        contrastLevel = contrastLevelDbl,
      ) {
        getSecondaryPalette(
          variant = colorStyle,
          sourceColorHct = sourceColor,
          isDark = isDark,
          platform = DynamicScheme.Platform.PHONE,
          contrastLevel = contrastLevelDbl,
        )
      },
    tertiaryPalette = ColorSpecs
      .get(colorSpec)
      .getOverrideOr(
        overrideColor = tertiaryColorOverride,
        colorStyle = colorStyle,
        isDark = isDark,
        contrastLevel = contrastLevelDbl,
      ) {
        getTertiaryPalette(
          variant = colorStyle,
          sourceColorHct = sourceColor,
          isDark = isDark,
          platform = DynamicScheme.Platform.PHONE,
          contrastLevel = contrastLevelDbl,
        )
      },
    neutralPalette = ColorSpecs
      .get(colorSpec)
      .getNeutralPalette(
        variant = colorStyle,
        sourceColorHct = neutralColorOverride ?: sourceColor,
        isDark = isDark,
        platform = DynamicScheme.Platform.PHONE,
        contrastLevel = contrastLevelDbl,
      ),
    neutralVariantPalette = ColorSpecs
      .get(colorSpec)
      .getNeutralVariantPalette(
        variant = colorStyle,
        sourceColorHct = neutralVariantColorOverride ?: sourceColor,
        isDark = isDark,
        platform = DynamicScheme.Platform.PHONE,
        contrastLevel = contrastLevelDbl,
      ),
    errorPalette = ColorSpecs
      .get(colorSpec)
      .getOverrideOr(
        overrideColor = errorColorOverride,
        colorStyle = colorStyle,
        isDark = isDark,
        contrastLevel = contrastLevelDbl,
      ) {
        getErrorPalette(
          variant = colorStyle,
          sourceColorHct = sourceColor,
          isDark = isDark,
          platform = DynamicScheme.Platform.PHONE,
          contrastLevel = contrastLevelDbl,
        )
      },
  )
}

fun ColorSpec.getOverrideOr(
  overrideColor: Hct?,
  colorStyle: Variant,
  isDark: Boolean,
  contrastLevel: Double,
  block: ColorSpec.() -> TonalPalette,
): TonalPalette {
  return if (overrideColor != null) {
    getPrimaryPalette(
      variant = colorStyle,
      sourceColorHct = overrideColor,
      isDark = isDark,
      platform = DynamicScheme.Platform.PHONE,
      contrastLevel = contrastLevel,
    )
  } else {
    block()
  }
}
