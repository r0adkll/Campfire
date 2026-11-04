// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.core.coroutines.LoadState
import app.campfire.settings.api.ThemeMode
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.platformColorPalette
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.campfire.ui.theming.ui.preview.PreviewLibrary
import app.campfire.ui.theming.ui.preview.PreviewShelf
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ThemePickerUiTest {

  private val phone = Modifier.size(width = 400.dp, height = 900.dp)
  private val ember = customTheme(id = "ember", name = "Ember")
  private val events = mutableListOf<ThemePickerUiEvent>()

  private fun state(
    customThemes: LoadState<out List<AppTheme.Fixed>> = LoadState.Loaded(listOf(ember, aiTheme())),
    aiThemeBuilderAvailable: Boolean = true,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    currentTheme: AppTheme = AppTheme.Fixed.Tent,
  ) = ThemePickerUiState(
    currentTheme = currentTheme,
    themeMode = themeMode,
    builtInThemes = BuiltInThemes,
    customThemes = customThemes,
    aiThemeBuilderAvailable = aiThemeBuilderAvailable,
    previewContent = PreviewContent(NoBooks),
    eventSink = { events += it },
  )

  /**
   * Previews without book cards. Composing many cards while the grid scrolls trips Compose's
   * concurrent-change check in desktop UI tests, where resources load off the UI thread; the
   * render tests draw the tiles with books.
   */
  private val NoBooks = PreviewLibrary(
    continueListening = PreviewShelf(label = null, items = emptyList()),
    recentlyAdded = PreviewShelf(label = null, items = emptyList()),
    itemCount = 0,
  )

  /** The app provides these; the picker's insets read them. */
  @Composable
  private fun Picker(state: ThemePickerUiState) {
    CompositionLocalProvider(
      LocalWindowSizeClass provides currentWindowSizeClass(),
      LocalContentLayout provides ContentLayout.Root,
    ) {
      ThemePicker(state, phone)
    }
  }

  @Test
  fun tiles_nameEachTheme_andMarkTheCurrentOne() = runComposeUiTest {
    setContent { Picker(state()) }

    onNode(hasContentDescription("Camping Tent Red") and isSelectable()).assertIsSelected()
    onNode(hasContentDescription("Forest Green") and isSelectable()).assertIsNotSelected()
    onNodeWithText("Built-in").assertExists()
  }

  @Test
  fun tiles_areOneNamedChoiceEach_withoutTheirPreviewsText() = runComposeUiTest {
    setContent { Picker(state()) }

    onNode(hasContentDescription("Camping Tent Red") and isSelectable()).assertExists()
    // The caption shows the name; the tile announces it, so it isn't read twice.
    onAllNodesWithText("Camping Tent Red").assertCountEquals(0)
    onAllNodesWithText("Search for your next story").assertCountEquals(0)
  }

  @Test
  fun tappingAPreviewOrItsName_picksItsTheme() = runComposeUiTest {
    setContent { Picker(state()) }

    // The node's center is on the preview, under the layer that takes its taps.
    onNode(hasContentDescription("Forest Green") and isSelectable()).performClick()
    // The bottom of the tile is its name.
    onNode(hasContentDescription("Water Bottle Blue") and isSelectable())
      .performTouchInput { click(percentOffset(0.5f, 0.95f)) }

    assertThat(events).containsExactly(
      ThemePickerUiEvent.SelectTheme(AppTheme.Fixed.Forest),
      ThemePickerUiEvent.SelectTheme(AppTheme.Fixed.WaterBottle),
    )
  }

  @Test
  fun modeButtons_showAndChangeTheSetting() = runComposeUiTest {
    setContent { Picker(state(themeMode = ThemeMode.SYSTEM)) }

    onNode(hasText("System") and isToggleable()).assertIsOn()
    onNode(hasText("Dark") and isToggleable()).assertIsOff()
    onNode(hasText("Dark") and isToggleable()).performClick()

    assertThat(events).containsExactly(ThemePickerUiEvent.SetThemeMode(ThemeMode.DARK))
  }

  @Test
  fun builtInThemes_canBeCustomized() = runComposeUiTest {
    setContent { Picker(state()) }

    // Customizing is a built-in theme's only action, so it's a button, not a menu of one.
    onNodeWithContentDescription("More options for Forest Green").assertDoesNotExist()
    onNodeWithContentDescription("Customize Forest Green").performClick()

    assertThat(events.single()).isInstanceOf<ThemePickerUiEvent.Customize>().all {
      prop(ThemePickerUiEvent.Customize::theme).isEqualTo(AppTheme.Fixed.Forest)
      prop(ThemePickerUiEvent.Customize::seedColor).isEqualTo(
        AppTheme.Fixed.Forest.platformColorPalette.lightColorScheme.primary,
      )
    }
  }

  @Test
  fun yourThemes_canBeEditedAndDuplicated() = runComposeUiTest {
    setContent { Picker(state()) }

    onNode(hasScrollAction()).performScrollToKey("custom:ember")
    onNodeWithText("Your themes").assertExists()
    onNode(hasContentDescription("Ember") and isSelectable()).assertIsNotSelected()
    onNodeWithContentDescription("More options for Ember").performClick()
    onNodeWithText("Customize").assertDoesNotExist()
    onNodeWithText("Edit").performClick()
    onNodeWithContentDescription("More options for Ember").performClick()
    onNodeWithText("Duplicate").performClick()

    assertThat(events).containsExactly(
      ThemePickerUiEvent.OpenThemeBuilder(ember),
      ThemePickerUiEvent.Duplicate(ember, "Ember copy"),
    )
  }

  @Test
  fun deletingATheme_asksFirst() = runComposeUiTest {
    setContent { Picker(state()) }

    onNode(hasScrollAction()).performScrollToKey("custom:ember")
    onNodeWithContentDescription("More options for Ember").performClick()
    onNodeWithText("Delete").performClick()
    onNodeWithText("Delete Ember?").assertExists()
    onNodeWithText("This can't be undone.").assertExists()
    onNodeWithText("Cancel").performClick()
    onNodeWithText("Delete Ember?").assertDoesNotExist()
    assertThat(events).isEmpty()

    onNodeWithContentDescription("More options for Ember").performClick()
    onNodeWithText("Delete").performClick()
    onNodeWithText("Delete").performClick()

    assertThat(events).containsExactly(ThemePickerUiEvent.Delete(ember))
    onNodeWithText("Delete Ember?").assertDoesNotExist()
  }

  @Test
  fun deletingTheThemeInUse_saysWhatTheAppSwitchesTo() = runComposeUiTest {
    setContent { Picker(state(currentTheme = ember)) }

    onNode(hasScrollAction()).performScrollToKey("custom:ember")
    onNodeWithContentDescription("More options for Ember").performClick()
    onNodeWithText("Delete").performClick()

    onNodeWithText("This can't be undone, and Campfire switches back to Camping Tent Red.").assertExists()
  }

  @Test
  fun createTiles_startNewThemes() = runComposeUiTest {
    setContent { Picker(state()) }

    onNode(hasScrollAction()).performScrollToKey("ai")
    onNodeWithText("New theme").performClick()
    onNodeWithText("Create with AI").performClick()

    assertThat(events).containsExactly(
      ThemePickerUiEvent.OpenThemeBuilder(),
      ThemePickerUiEvent.OpenAiThemeBuilder,
    )
  }

  @Test
  fun createWithAi_isHiddenUntilItsModelIsReady() = runComposeUiTest {
    setContent { Picker(state(aiThemeBuilderAvailable = false)) }

    onNode(hasScrollAction()).performScrollToKey("new")
    onNodeWithText("New theme").assertExists()
    onNodeWithText("Create with AI").assertDoesNotExist()
  }

  @Test
  fun yourThemes_sayWhenTheyCantLoad() = runComposeUiTest {
    setContent { Picker(state(customThemes = LoadState.Error)) }

    onNode(hasScrollAction()).performScrollToKey("error")
    onNodeWithText("Your themes couldn't be loaded").assertExists()
    onNodeWithContentDescription("More options for Ember").assertDoesNotExist()
  }
}
