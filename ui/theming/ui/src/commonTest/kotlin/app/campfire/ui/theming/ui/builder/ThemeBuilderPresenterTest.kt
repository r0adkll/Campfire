// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.ui.graphics.Color
import app.campfire.common.screens.SettingsScreen
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.cash.turbine.ReceiveTurbine
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
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
import kotlinx.coroutines.test.runTest

class ThemeBuilderPresenterTest {

  private val repository = FakeAppThemeRepository()
  private val navigator = FakeNavigator(ThemeBuilderScreen())
  private val previewContent = MutableStateFlow(PreviewContent())

  private fun presenter(screen: ThemeBuilderScreen = ThemeBuilderScreen()) = ThemeBuilderPresenter(
    themeRepository = repository,
    previewContentSource = { previewContent },
    screen = screen,
    navigator = navigator,
  )

  @Test
  fun newTheme_startsAsExpressiveOnThe2025Spec() = runTest {
    presenter().test {
      assertThat(awaitItem()).all {
        prop(ThemeBuilderUiState::colorStyle).isEqualTo(Variant.EXPRESSIVE)
        prop(ThemeBuilderUiState::colorSpec).isEqualTo(SpecVersion.SPEC_2025)
        prop(ThemeBuilderUiState::isNew).isTrue()
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun state_offersEveryStylePaletteAndCheck() = runTest {
    presenter().test {
      val state = awaitItem()
      assertThat(state.styles.map { it.style }).containsExactly(*StyleOrder.toTypedArray())
      assertThat(state.palettes.map { it.key }).containsExactly(*PaletteKey.entries.toTypedArray())
      assertThat(state.contrast.map { it.check }).containsExactly(*ContrastCheck.entries.toTypedArray())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun customThemeCount_followsTheRepository() = runTest {
    repository.themes["a"] = customTheme(id = "a", name = "A")
    repository.themes["b"] = customTheme(id = "b", name = "B")

    presenter().test {
      assertThat(awaitState { it.customThemeCount == 2 }.customThemeCount).isEqualTo(2)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun paletteColorPicks_applyTheirOverrides() = runTest {
    presenter().test {
      val sink = awaitItem().eventSink
      sink(ThemeBuilderUiEvent.ColorPicked(PaletteKey.Supporting, Color.Green))
      sink(ThemeBuilderUiEvent.ColorPicked(PaletteKey.ThirdAccent, Color.Blue))
      sink(ThemeBuilderUiEvent.ColorPicked(PaletteKey.Errors, Color.Magenta))
      sink(ThemeBuilderUiEvent.IconPicked(AppTheme.Icon.Owl))

      val state = awaitState { it.theme.errorColorOverride != null && it.theme.icon == AppTheme.Icon.Owl }
      assertThat(state.theme).all {
        prop(AppTheme.Fixed.Custom::secondaryColorOverride).isEqualTo(Color.Green)
        prop(AppTheme.Fixed.Custom::tertiaryColorOverride).isEqualTo(Color.Blue)
        prop(AppTheme.Fixed.Custom::errorColorOverride).isEqualTo(Color.Magenta)
      }
      assertThat(state.palettes.single { it.key == PaletteKey.ThirdAccent }).all {
        prop(PaletteRow::isCustom).isTrue()
        prop(PaletteRow::keyColor).isEqualTo(Color.Blue)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun pickingTheAccent_keepsOtherPicks() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemeBuilderUiEvent.ColorPicked(PaletteKey.ThirdAccent, Color.Blue))
      awaitState { it.theme.tertiaryColorOverride != null }
        .eventSink(ThemeBuilderUiEvent.ColorPicked(PaletteKey.Accent, Color.Green))

      val state = awaitState { it.seedColor == Color.Green }
      assertThat(state.theme.tertiaryColorOverride).isEqualTo(Color.Blue)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun backgrounds_setAndResetBothNeutralPalettes() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemeBuilderUiEvent.ColorPicked(PaletteKey.Backgrounds, Color.Cyan))

      val picked = awaitState { it.theme.neutralColorOverride != null && it.theme.neutralVariantColorOverride != null }
      assertThat(picked.theme).all {
        prop(AppTheme.Fixed.Custom::neutralColorOverride).isEqualTo(Color.Cyan)
        prop(AppTheme.Fixed.Custom::neutralVariantColorOverride).isEqualTo(Color.Cyan)
      }

      picked.eventSink(ThemeBuilderUiEvent.ColorReset(PaletteKey.Backgrounds))
      val reset = awaitState { it.theme.neutralColorOverride == null && it.theme.neutralVariantColorOverride == null }
      assertThat(reset.palettes.single { it.key == PaletteKey.Backgrounds }.isCustom).isEqualTo(false)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun resettingTheAccent_keepsTheSeed() = runTest {
    presenter().test {
      val state = awaitItem()
      state.eventSink(ThemeBuilderUiEvent.ColorReset(PaletteKey.Accent))
      state.eventSink(ThemeBuilderUiEvent.IconPicked(AppTheme.Icon.Owl))

      assertThat(awaitState { it.theme.icon == AppTheme.Icon.Owl }.seedColor).isEqualTo(state.seedColor)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun styleClick_bringsTheStylesSpec() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemeBuilderUiEvent.StyleClick(Variant.FIDELITY))
      val classic = awaitState { it.colorStyle == Variant.FIDELITY }
      assertThat(classic.colorSpec).isEqualTo(SpecVersion.SPEC_2021)

      classic.eventSink(ThemeBuilderUiEvent.StyleClick(Variant.VIBRANT))
      assertThat(awaitState { it.colorStyle == Variant.VIBRANT }.colorSpec).isEqualTo(SpecVersion.SPEC_2025)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun contrastLevelClick_updatesTheTheme() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemeBuilderUiEvent.ContrastLevelClick(ContrastLevel.High))

      val state = awaitState { it.contrastLevel == ContrastLevel.High }
      assertThat(state.theme.contrastLevel).isEqualTo(1f)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun tertiaryPick_rebuildsThePalette() = runTest {
    presenter().test {
      val initial = awaitItem()
      initial.eventSink(ThemeBuilderUiEvent.ColorPicked(PaletteKey.ThirdAccent, Color(0xFF00A3FF)))

      val updated = awaitState { it.theme.tertiaryColorOverride != null }
      assertThat(updated.theme.colorPalette.lightColorScheme.tertiary)
        .isNotEqualTo(initial.theme.colorPalette.lightColorScheme.tertiary)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun existingTheme_loadsItsChoicesAndKeepsItsSpec() = runTest {
    // Expressive on the 2021 spec predates the spec following the style; it must load unchanged.
    val saved = customTheme(id = "ember", name = "Ember", style = Variant.EXPRESSIVE, spec = SpecVersion.SPEC_2021)
    repository.themes[saved.id] = saved

    presenter(ThemeBuilderScreen(customThemeId = saved.id)).test {
      val state = awaitState { it.theme.name == "Ember" && it.theme.tertiaryColorOverride != null }
      assertThat(state).all {
        prop(ThemeBuilderUiState::isNew).isEqualTo(false)
        prop(ThemeBuilderUiState::colorStyle).isEqualTo(Variant.EXPRESSIVE)
        prop(ThemeBuilderUiState::colorSpec).isEqualTo(SpecVersion.SPEC_2021)
        prop(ThemeBuilderUiState::seedColor).isEqualTo(saved.seedColor)
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
  fun save_existingTheme_keepsItsId() = runTest {
    val saved = customTheme(id = "ember", name = "Ember")
    repository.themes[saved.id] = saved

    presenter(ThemeBuilderScreen(customThemeId = saved.id)).test {
      awaitState { it.theme.name == "Ember" }.eventSink(ThemeBuilderUiEvent.Save)
      navigator.awaitPop()

      assertThat(repository::saved).single().prop(AppTheme.Fixed::id).isEqualTo("ember")
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

  @Test
  fun delete_newTheme_doesNothing() = runTest {
    presenter().test {
      val state = awaitItem()
      state.eventSink(ThemeBuilderUiEvent.Delete)
      state.eventSink(ThemeBuilderUiEvent.Back)
      navigator.awaitPop()

      assertThat(repository::deleted).hasSize(0)
      assertThat(repository.themes["new-theme-id"]).isNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun previewContent_followsTheSource() = runTest {
    presenter().test {
      assertThat(awaitItem().previewContent).isEqualTo(PreviewContent())

      val content = PreviewContent(playerUsesCoverColors = false)
      previewContent.value = content

      assertThat(awaitState { it.previewContent == content }.previewContent).isEqualTo(content)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun coverThemingSettings_opensAppearanceSettings() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemeBuilderUiEvent.CoverThemingSettings)

      assertThat(navigator.awaitNextScreen()).isEqualTo(SettingsScreen(SettingsScreen.Page.Appearance))
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
  private val customThemes = MutableStateFlow<List<AppTheme.Fixed>>(emptyList())

  override fun observeCurrentAppTheme(): StateFlow<AppTheme> = MutableStateFlow(AppTheme.Fixed.Tent)

  override fun observeCustomThemes(): Flow<List<AppTheme.Fixed>> {
    customThemes.value = themes.values.toList()
    return customThemes
  }

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
