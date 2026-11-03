// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.ui.graphics.Color
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import app.cash.turbine.ReceiveTurbine
import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.single
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ThemeBuilderPresenterTest {

  private val repository = FakeAppThemeRepository()
  private val navigator = FakeNavigator(ThemeBuilderScreen())

  private fun presenter(screen: ThemeBuilderScreen = ThemeBuilderScreen()) = ThemeBuilderPresenter(
    themeRepository = repository,
    screen = screen,
    navigator = navigator,
  )

  @Test
  fun newTheme_startsAsExpressiveOnThe2025Spec() = runTest {
    presenter().test {
      assertThat(awaitItem()).all {
        prop(ThemeBuilderUiState::colorStyle).isEqualTo(Variant.EXPRESSIVE)
        prop(ThemeBuilderUiState::colorSpec).isEqualTo(SpecVersion.SPEC_2025)
        prop(ThemeBuilderUiState::theme).prop(AppTheme.Fixed.Custom::isNew).isTrue()
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun paletteColorPicks_applyTheirOverrides() = runTest {
    presenter().test {
      val sink = awaitItem().eventSink
      sink(ThemeBuilderUiEvent.SecondaryColorPicked(Color.Green))
      sink(ThemeBuilderUiEvent.ErrorColorPicked(Color.Magenta))
      sink(ThemeBuilderUiEvent.NeutralColorPicked(Color.Cyan))
      sink(ThemeBuilderUiEvent.IconPicked(AppTheme.Icon.Owl))

      val state = awaitState { it.neutralColorOverride != null && it.theme.icon == AppTheme.Icon.Owl }
      assertThat(state).all {
        prop(ThemeBuilderUiState::secondaryColorOverride).isEqualTo(Color.Green)
        prop(ThemeBuilderUiState::errorColorOverride).isEqualTo(Color.Magenta)
        prop(ThemeBuilderUiState::neutralColorOverride).isEqualTo(Color.Cyan)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun seedColorPicked_clearsPaletteOverrides() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemeBuilderUiEvent.TertiaryColorPicked(Color.Blue))
      awaitState { it.tertiaryColorOverride != null }.eventSink(ThemeBuilderUiEvent.SeedColorPicked(Color.Green))

      // The test clock can recompose between the presenter's writes, so wait for the settled state.
      val state = awaitState { it.seedColor == Color.Green && it.tertiaryColorOverride == null }
      assertThat(state.theme).all {
        prop(AppTheme.Fixed.Custom::seedColor).isEqualTo(Color.Green)
        prop(AppTheme.Fixed.Custom::tertiaryColorOverride).isEqualTo(null)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun styleSpecAndContrastClicks_updateTheTheme() = runTest {
    presenter().test {
      val sink = awaitItem().eventSink
      sink(ThemeBuilderUiEvent.ColorSpecClick(SpecVersion.SPEC_2021))
      sink(ThemeBuilderUiEvent.ColorStyleClick(Variant.FIDELITY))
      sink(ThemeBuilderUiEvent.ContrastLevelClick(ContrastLevel.High))

      val state = awaitState { it.colorStyle == Variant.FIDELITY && it.contrastLevel == ContrastLevel.High }
      assertThat(state.theme).all {
        prop(AppTheme.Fixed.Custom::colorSpec).isEqualTo(SpecVersion.SPEC_2021)
        prop(AppTheme.Fixed.Custom::colorStyle).isEqualTo(Variant.FIDELITY)
        prop(AppTheme.Fixed.Custom::contrastLevel).isEqualTo(1f)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun switchingTo2025Spec_replacesAClassicStyle() = runTest {
    presenter().test {
      val sink = awaitItem().eventSink
      sink(ThemeBuilderUiEvent.ColorSpecClick(SpecVersion.SPEC_2021))
      sink(ThemeBuilderUiEvent.ColorStyleClick(Variant.FIDELITY))
      awaitState { it.colorStyle == Variant.FIDELITY }.eventSink(
        ThemeBuilderUiEvent.ColorSpecClick(SpecVersion.SPEC_2025),
      )

      val state = awaitState { it.colorSpec == SpecVersion.SPEC_2025 && it.colorStyle == Variant.EXPRESSIVE }
      assertThat(state.colorStyle).isEqualTo(Variant.EXPRESSIVE)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun back_popsTheScreen() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemeBuilderUiEvent.Back)
      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun neutralVariantColorPicked_appliesTheOverride() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemeBuilderUiEvent.NeutralVariantColorPicked(Color.Blue))

      val state = awaitState { it.neutralVariantColorOverride != null }
      assertThat(state).all {
        prop(ThemeBuilderUiState::neutralVariantColorOverride).isEqualTo(Color.Blue)
        prop(ThemeBuilderUiState::theme)
          .prop(AppTheme.Fixed.Custom::neutralVariantColorOverride)
          .isEqualTo(Color.Blue)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun tertiaryColorPicked_rebuildsThePalette() = runTest {
    presenter().test {
      val initial = awaitItem()
      initial.eventSink(ThemeBuilderUiEvent.TertiaryColorPicked(Color(0xFF00A3FF)))

      val updated = awaitState { it.tertiaryColorOverride != null }
      assertThat(updated.theme.colorPalette.lightColorScheme.tertiary)
        .isNotEqualTo(initial.theme.colorPalette.lightColorScheme.tertiary)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun existingTheme_loadsItsChoices() = runTest {
    val saved = customTheme(id = "ember", name = "Ember", style = Variant.FIDELITY, spec = SpecVersion.SPEC_2021)
    repository.themes[saved.id] = saved

    presenter(ThemeBuilderScreen(customThemeId = saved.id)).test {
      val state = awaitState { it.theme.name == "Ember" && it.tertiaryColorOverride != null }
      assertThat(state).all {
        prop(ThemeBuilderUiState::colorStyle).isEqualTo(Variant.FIDELITY)
        prop(ThemeBuilderUiState::colorSpec).isEqualTo(SpecVersion.SPEC_2021)
        prop(ThemeBuilderUiState::seedColor).isEqualTo(saved.seedColor)
        prop(ThemeBuilderUiState::tertiaryColorOverride).isEqualTo(saved.tertiaryColorOverride)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun save_newTheme_storesItUnderAFreshId() = runTest {
    presenter().test {
      val state = awaitItem()
      state.name.edit { replace(0, length, "Ember") }
      awaitState { it.theme.name == "Ember" }.eventSink(ThemeBuilderUiEvent.Save)
      navigator.awaitPop()

      assertThat(repository::saved).single().all {
        prop(AppTheme.Fixed::id).isNotEqualTo(NEW_THEME_ID)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun delete_existingTheme_removesIt() = runTest {
    val saved = customTheme(id = "ember", name = "Ember")
    repository.themes[saved.id] = saved

    presenter(ThemeBuilderScreen(customThemeId = saved.id)).test {
      awaitState { it.theme.name == "Ember" }.eventSink(ThemeBuilderUiEvent.Delete)
      navigator.awaitPop()

      assertThat(repository::deleted).hasSize(1)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private suspend fun ReceiveTurbine<ThemeBuilderUiState>.awaitState(
    predicate: (ThemeBuilderUiState) -> Boolean,
  ): ThemeBuilderUiState {
    while (true) {
      val state = awaitItem()
      if (predicate(state)) return state
    }
  }

  private fun customTheme(
    id: String,
    name: String,
    style: Variant = Variant.TONAL_SPOT,
    spec: SpecVersion = SpecVersion.SPEC_2025,
  ): AppTheme.Fixed.Custom {
    val seed = Color(0xFFC0532D)
    val tertiary = Color(0xFF00A3FF)
    return AppTheme.Fixed.Custom(
      id = id,
      name = name,
      seedColor = seed,
      colorSpec = spec,
      colorStyle = style,
      contrastLevel = 0f,
      secondaryColorOverride = null,
      tertiaryColorOverride = tertiary,
      errorColorOverride = null,
      neutralColorOverride = null,
      neutralVariantColorOverride = null,
      icon = AppTheme.Icon.Campfire,
      colorPalette = CustomThemeColors(seed, style, spec, tertiaryColor = tertiary).toColorPalette(),
    )
  }
}

private class FakeAppThemeRepository : AppThemeRepository {
  val themes = mutableMapOf<String, AppTheme.Fixed>()
  val saved = mutableListOf<AppTheme.Fixed>()
  val deleted = mutableListOf<String>()

  override fun observeCurrentAppTheme(): StateFlow<AppTheme> = MutableStateFlow(AppTheme.Fixed.Tent)

  override fun observeCustomThemes(): Flow<List<AppTheme.Fixed>> = flowOf(themes.values.toList())

  override fun setCurrentTheme(theme: AppTheme) = Unit

  override suspend fun getCustomTheme(id: String): Result<AppTheme.Fixed> =
    themes[id]?.let { Result.success(it) } ?: Result.failure(NoSuchElementException(id))

  override suspend fun saveCustomTheme(theme: AppTheme.Fixed) {
    saved += theme
    themes[theme.id] = theme
  }

  override suspend fun deleteCustomTheme(id: String) {
    deleted += id
    themes.remove(id)
  }
}
