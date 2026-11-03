// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.ui.graphics.Color
import app.campfire.common.screens.SettingsScreen
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.RoleOverrides
import app.campfire.ui.theming.api.ThemeManager
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import app.campfire.ui.theming.test.FakeThemeManager
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import app.campfire.ui.theming.ui.builder.color.TEXT_CONTRAST
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.campfire.ui.theming.ui.preview.previewLibrary
import app.cash.turbine.ReceiveTurbine
import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.single
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec.SpecVersion
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.r0adkll.swatchbuckler.compose.Swatch
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
  private val themeManager = SwatchThemeManager()

  private fun presenter(screen: ThemeBuilderScreen = ThemeBuilderScreen()) = ThemeBuilderPresenter(
    themeRepository = repository,
    previewContentSource = { previewContent },
    themeManager = themeManager,
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
  fun roleColorPicked_tunesThePreviewButNotTheControls() = runTest {
    presenter().test {
      val initial = awaitItem()
      initial.eventSink(ThemeBuilderUiEvent.RoleColorPicked(ThemeRole.Primary, dark = false, color = Color.Magenta))

      val tuned = awaitState { !it.roleOverrides.isEmpty }
      assertThat(tuned.roleOverrides).isEqualTo(RoleOverrides(light = mapOf(ThemeRole.Primary to Color.Magenta)))
      assertThat(tuned.theme.colorPalette.lightColorScheme.primary).isEqualTo(Color.Magenta)
      assertThat(tuned.theme.colorPalette.darkColorScheme.primary)
        .isEqualTo(initial.theme.colorPalette.darkColorScheme.primary)
      assertThat(tuned.generatedPalette.lightColorScheme.primary)
        .isEqualTo(initial.generatedPalette.lightColorScheme.primary)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun rolesReset_putsTheGeneratedColorsBackInThatModeOnly() = runTest {
    presenter().test {
      val sink = awaitItem().eventSink
      sink(ThemeBuilderUiEvent.RoleColorPicked(ThemeRole.Surface, dark = false, color = Color.Yellow))
      sink(ThemeBuilderUiEvent.RoleColorPicked(ThemeRole.Surface, dark = true, color = Color.DarkGray))
      sink(ThemeBuilderUiEvent.RolesReset(setOf(ThemeRole.Surface, ThemeRole.OnSurface), dark = false))

      val state = awaitState { it.roleOverrides.light.isEmpty() && it.roleOverrides.dark.isNotEmpty() }
      assertThat(state.roleOverrides).isEqualTo(RoleOverrides(dark = mapOf(ThemeRole.Surface to Color.DarkGray)))
      assertThat(state.theme.colorPalette.lightColorScheme.surface)
        .isEqualTo(state.generatedPalette.lightColorScheme.surface)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun fixContrast_makesAHardToReadPartReadable() = runTest {
    presenter().test {
      val initial = awaitItem()
      val cardColor = initial.generatedPalette.lightColorScheme.surfaceContainerHigh
      initial.eventSink(ThemeBuilderUiEvent.RoleColorPicked(ThemeRole.OnSurface, dark = false, color = cardColor))
      val failing = awaitState { ContrastCheck.CardText in it.failingChecks(dark = false) }

      failing.eventSink(ThemeBuilderUiEvent.FixContrast(ContrastCheck.CardText, dark = false))

      val fixed = awaitState { ContrastCheck.CardText !in it.failingChecks(dark = false) }
      assertThat(fixed.contrast.single { it.check == ContrastCheck.CardText }.lightRatio)
        .isGreaterThanOrEqualTo(TEXT_CONTRAST)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun mirrorRoles_copiesFineTunesToTheOtherMode() = runTest {
    presenter().test {
      awaitItem().eventSink(
        ThemeBuilderUiEvent.RoleColorPicked(ThemeRole.SurfaceContainerHigh, dark = false, color = Color(0xFFFFF3E0)),
      )
      awaitState { !it.roleOverrides.isEmpty }.eventSink(
        ThemeBuilderUiEvent.MirrorRoles(
          roles = setOf(ThemeRole.SurfaceContainerHigh, ThemeRole.OnSurface),
          fromDark = false,
          check = ContrastCheck.CardText,
        ),
      )

      val mirrored = awaitState { it.roleOverrides.dark.isNotEmpty() }
      assertThat(mirrored.roleOverrides.dark.keys).contains(ThemeRole.SurfaceContainerHigh)
      assertThat(ContrastCheck.CardText in mirrored.failingChecks(dark = true)).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun fineTunes_outlastStyleChanges() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemeBuilderUiEvent.RoleColorPicked(ThemeRole.Primary, dark = true, color = Color.Cyan))
      awaitState { !it.roleOverrides.isEmpty }.eventSink(ThemeBuilderUiEvent.StyleClick(Variant.VIBRANT))

      val restyled = awaitState { it.colorStyle == Variant.VIBRANT }
      assertThat(restyled.theme.colorPalette.darkColorScheme.primary).isEqualTo(Color.Cyan)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun existingTheme_loadsItsFineTunes() = runTest {
    val overrides = RoleOverrides(light = mapOf(ThemeRole.Outline to Color.Gray))
    val saved = customTheme(id = "ember", name = "Ember").copy(roleOverrides = overrides)
    repository.themes[saved.id] = saved

    presenter(ThemeBuilderScreen(customThemeId = saved.id)).test {
      val state = awaitState { it.theme.name == "Ember" && !it.roleOverrides.isEmpty }
      assertThat(state.roleOverrides).isEqualTo(overrides)
      assertThat(state.theme.colorPalette.lightColorScheme.outline).isEqualTo(Color.Gray)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun save_keepsTheFineTunes() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemeBuilderUiEvent.RoleColorPicked(ThemeRole.Primary, dark = false, color = Color.Magenta))
      awaitState { !it.roleOverrides.isEmpty }.eventSink(ThemeBuilderUiEvent.Save)
      navigator.awaitPop()

      val saved = repository.saved.single() as AppTheme.Fixed.Custom
      assertThat(saved.roleOverrides).isEqualTo(RoleOverrides(light = mapOf(ThemeRole.Primary to Color.Magenta)))
      assertThat(saved.colorPalette.lightColorScheme.primary).isEqualTo(Color.Magenta)
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
  fun coverColors_comeFromTheUsersBooksAsTheirCoversAreRead() = runTest {
    previewContent.value = PreviewContent(previewLibrary())

    presenter().test {
      assertThat(awaitState { it.previewContent.library.continueListening.items.isNotEmpty() }.coverColors)
        .isEqualTo(emptyList())

      themeManager.swatch("li_piranesi").value = Swatch(dominant = Color(0xFF2E5E4E))
      themeManager.swatch("li_hail_mary").value = Swatch(dominant = Color(0xFFB8860B))

      val state = awaitState { it.coverColors.size == 2 }
      // In shelf order, not in the order their covers were read.
      assertThat(state.coverColors.map { it.item.id to it.color }).containsExactly(
        "li_hail_mary" to Color(0xFFB8860B),
        "li_piranesi" to Color(0xFF2E5E4E),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun coverColors_skipSamples() = runTest {
    themeManager.swatch("sample_moby_dick").value = Swatch(dominant = Color.Red)

    presenter().test {
      val state = awaitItem()
      assertThat(state.previewContent.library.isEmpty).isFalse()
      assertThat(state.coverColors).isEqualTo(emptyList())
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

/** Each book's cover swatch, set by the test as covers would be read. */
private class SwatchThemeManager : ThemeManager by FakeThemeManager() {
  private val swatches = mutableMapOf<String, MutableStateFlow<Swatch?>>()

  fun swatch(itemId: String): MutableStateFlow<Swatch?> = swatches.getOrPut(itemId) { MutableStateFlow(null) }

  override fun observeSwatchFor(key: String): Flow<Swatch?> = swatch(key)
}
