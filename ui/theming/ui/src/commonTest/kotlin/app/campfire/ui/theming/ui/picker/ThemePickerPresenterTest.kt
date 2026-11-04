// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import androidx.compose.ui.graphics.Color
import app.campfire.core.coroutines.LoadState
import app.campfire.settings.api.ThemeMode
import app.campfire.settings.test.TestThemeSettings
import app.campfire.ui.theming.api.AiThemeBuilder
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.NoOpAiThemeBuilder
import app.campfire.ui.theming.api.screen.AiThemeBuilderScreen
import app.campfire.ui.theming.api.screen.ThemeBuilderScreen
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import app.campfire.ui.theming.test.FakeAppThemeRepository
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.campfire.ui.theming.ui.preview.previewLibrary
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class ThemePickerPresenterTest {

  private val repository = FakeAppThemeRepository()
  private val navigator = FakeNavigator(ThemePickerScreen)
  private val previewContent = MutableStateFlow(PreviewContent())

  private fun TestScope.presenter(
    settings: TestThemeSettings = TestThemeSettings(backgroundScope),
    aiThemeBuilder: AiThemeBuilder = NoOpAiThemeBuilder,
  ) = ThemePickerPresenter(
    appThemeRepository = repository,
    themeSettings = settings,
    previewContentSource = { previewContent },
    navigator = navigator,
    aiThemeBuilder = aiThemeBuilder,
  )

  @Test
  fun builtInThemes_comeFirstAndInOrder() = runTest {
    presenter().test {
      assertThat(awaitItem().builtInThemes.take(6)).containsExactly(
        AppTheme.Fixed.Tent,
        AppTheme.Fixed.Forest,
        AppTheme.Fixed.WaterBottle,
        AppTheme.Fixed.Rucksack,
        AppTheme.Fixed.LifeFloat,
        AppTheme.Fixed.Mountain,
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun customThemes_followTheRepository() = runTest {
    val ember = customTheme(id = "ember", name = "Ember")
    repository.themes[ember.id] = ember

    presenter().test {
      val state = awaitState { it.customThemes is LoadState.Loaded }
      assertThat(state.customThemes).isInstanceOf<LoadState.Loaded<List<AppTheme.Fixed>>>()
        .prop(LoadState.Loaded<List<AppTheme.Fixed>>::data)
        .containsExactly(ember)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun selectingATheme_appliesIt() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemePickerUiEvent.SelectTheme(AppTheme.Fixed.Forest))

      assertThat(awaitState { it.currentTheme == AppTheme.Fixed.Forest }.currentTheme)
        .isEqualTo(AppTheme.Fixed.Forest)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun themeMode_writesTheSharedSetting() = runTest {
    val settings = TestThemeSettings(backgroundScope)
    presenter(settings).test {
      awaitItem().eventSink(ThemePickerUiEvent.SetThemeMode(ThemeMode.DARK))

      assertThat(awaitState { it.themeMode == ThemeMode.DARK }.themeMode).isEqualTo(ThemeMode.DARK)
      assertThat(settings.themeMode).isEqualTo(ThemeMode.DARK)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun previewContent_followsTheSource() = runTest {
    presenter().test {
      awaitItem()
      val library = previewLibrary()
      previewContent.value = PreviewContent(library)

      assertThat(awaitState { it.previewContent.library == library }.previewContent.library)
        .isEqualTo(library)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun createWithAi_showsOnlyWhileTheModelIsReady() = runTest {
    val ai = FakeAiThemeBuilder()
    presenter(aiThemeBuilder = ai).test {
      assertThat(awaitItem().aiThemeBuilderAvailable).isFalse()
      ai.available.value = true
      assertThat(awaitState { it.aiThemeBuilderAvailable }.aiThemeBuilderAvailable).isTrue()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun openingTheBuilder_routesByKindOfTheme() = runTest {
    val custom = customTheme(id = "ember", name = "Ember")
    val ai = aiTheme()
    presenter(aiThemeBuilder = FakeAiThemeBuilder(isSupported = true)).test {
      val sink = awaitItem().eventSink
      sink(ThemePickerUiEvent.OpenThemeBuilder())
      assertThat(navigator.awaitNextScreen()).isEqualTo(ThemeBuilderScreen())
      sink(ThemePickerUiEvent.OpenThemeBuilder(custom))
      assertThat(navigator.awaitNextScreen()).isEqualTo(ThemeBuilderScreen(custom.id))
      sink(ThemePickerUiEvent.OpenThemeBuilder(ai))
      assertThat(navigator.awaitNextScreen()).isEqualTo(AiThemeBuilderScreen(ai))
      sink(ThemePickerUiEvent.OpenAiThemeBuilder)
      assertThat(navigator.awaitNextScreen()).isEqualTo(AiThemeBuilderScreen())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun aiThemes_openInTheRegularBuilder_withoutTheAiModule() = runTest {
    val ai = aiTheme()
    presenter().test {
      awaitItem().eventSink(ThemePickerUiEvent.OpenThemeBuilder(ai))
      assertThat(navigator.awaitNextScreen()).isEqualTo(ThemeBuilderScreen(ai.id))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun back_leavesThePicker() = runTest {
    presenter().test {
      awaitItem().eventSink(ThemePickerUiEvent.Back)
      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun themeKeys_matchACustomThemeAcrossReloads() {
    val saved = customTheme(id = "ember", name = "Ember")
    val reloaded = saved.copy(colorPalette = CustomThemeColors(Color.Red, Variant.TONAL_SPOT).toColorPalette())
    assertThat(reloaded).isNotEqualTo(saved)
    assertThat(reloaded.key).isEqualTo(saved.key)
    assertThat(AppTheme.Dynamic.key).isNotEqualTo(AppTheme.Fixed.Tent.key)
  }

  private suspend fun ReceiveTurbine<ThemePickerUiState>.awaitState(
    predicate: (ThemePickerUiState) -> Boolean,
  ): ThemePickerUiState {
    while (true) {
      val state = awaitItem()
      if (predicate(state)) return state
    }
  }
}

private class FakeAiThemeBuilder(override val isSupported: Boolean = true) : AiThemeBuilder {
  val available = MutableStateFlow(false)
  override fun observeIsAvailable(): StateFlow<Boolean> = available
}
