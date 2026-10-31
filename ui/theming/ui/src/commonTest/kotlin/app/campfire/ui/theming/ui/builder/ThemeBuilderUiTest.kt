// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ThemeBuilderUiTest {

  private val phone = Modifier.size(width = 400.dp, height = 900.dp)
  private val wide = Modifier.size(width = 1200.dp, height = 800.dp)

  /** The app provides these; the builder's insets read them. */
  @Composable
  private fun Builder(state: ThemeBuilderUiState, modifier: Modifier) {
    CompositionLocalProvider(
      LocalWindowSizeClass provides currentWindowSizeClass(),
      LocalContentLayout provides ContentLayout.Root,
    ) {
      ThemeBuilder(state, modifier)
    }
  }

  @Test
  fun phone_showsThePreviewAboveTheControls() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNodeWithText("New theme").assertExists()
    onNodeWithText("Continue Listening").assertExists()
    onNodeWithText("Tap to change").assertExists()
    onNodeWithText("Readability").performScrollTo()
    onNodeWithText("Palette colors").performScrollTo()
    onNode(isToggleable() and hasText("Home")).assertExists()
    onNodeWithContentDescription("Preview in dark mode").assertExists()
  }

  @Test
  fun wide_showsLightAndDarkPreviewsWithoutAModeSwitch() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), wide) }

    onNodeWithText("Light").assertExists()
    onNodeWithText("Dark").assertExists()
    onNodeWithContentDescription("Preview in dark mode").assertDoesNotExist()
  }

  @Test
  fun styleCard_sendsTheStyle() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNodeWithText("Vibrant").performScrollTo().performClick()

    assertEquals(ThemeBuilderUiEvent.StyleClick(Variant.VIBRANT), events.last())
  }

  @Test
  fun swatch_picksTheAccent() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNodeWithContentDescription("Use #2563EB").performScrollTo().performClick()

    assertEquals(ThemeBuilderUiEvent.ColorPicked(PaletteKey.Accent, Color(0xFF2563EB)), events.last())
  }

  @Test
  fun contrastLevel_sendsTheLevel() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNodeWithText("High").performScrollTo().performClick()

    assertEquals(ThemeBuilderUiEvent.ContrastLevelClick(ContrastLevel.High), events.last())
  }

  @Test
  fun paletteRow_opensThePickerInPlaceOfTheControls() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNodeWithText("Supporting").performScrollTo().performClick()
    onNodeWithText("Hue").assertExists()
    onNodeWithText("Colorfulness").assertExists()
    onNodeWithText("Continue Listening").assertExists()

    onNodeWithText("Done").performClick()
    onNodeWithText("Hue").assertDoesNotExist()
    onNodeWithText("Tap to change").assertExists()
  }

  @Test
  fun picker_typedHexPicksThatColor() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNodeWithText("Tap to change").performClick()
    onNode(hasSetTextAction()).performTextReplacement("2563EB")
    waitForIdle()

    assertEquals(ThemeBuilderUiEvent.ColorPicked(PaletteKey.Accent, Color(0xFF2563EB)), events.last())
  }

  @Test
  fun picker_draggingHuePicksAColor() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNodeWithText("Tap to change").performClick()
    onNodeWithContentDescription("Hue").performSemanticsAction(SemanticsActions.SetProgress) { it(200f) }

    val pick = assertIs<ThemeBuilderUiEvent.ColorPicked>(events.last())
    assertEquals(PaletteKey.Accent, pick.key)
  }

  @Test
  fun picker_offersUseGeneratedForACustomPalette() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent {
      Builder(
        rememberSampleThemeBuilderState(
          colors = CustomThemeColors(Color(0xFFC0532D), Variant.EXPRESSIVE, secondaryColor = Color(0xFF16A34A)),
          eventSink = { events += it },
        ),
        phone,
      )
    }

    onNodeWithText("Supporting").performScrollTo().performClick()
    onNodeWithText("Use generated").performClick()

    assertEquals(ThemeBuilderUiEvent.ColorReset(PaletteKey.Supporting), events.last())
    onNodeWithText("Hue").assertDoesNotExist()
  }

  @Test
  fun paletteRow_resetButtonClearsACustomPalette() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent {
      Builder(
        rememberSampleThemeBuilderState(
          colors = CustomThemeColors(Color(0xFFC0532D), Variant.EXPRESSIVE, errorColor = Color(0xFFD946EF)),
          eventSink = { events += it },
        ),
        phone,
      )
    }

    onNodeWithContentDescription("Use the generated color").performScrollTo().performClick()

    assertEquals(ThemeBuilderUiEvent.ColorReset(PaletteKey.Errors), events.last())
  }

  @Test
  fun readability_listsEachFailingPartWithItsRatio() = runComposeUiTest {
    setContent {
      val state = rememberSampleThemeBuilderState()
      Builder(
        state.copy(
          contrast = state.contrast.map {
            if (it.check == ContrastCheck.Buttons) {
              it.copy(lightRatio = 1.46, darkRatio = 7.0)
            } else {
              it.copy(lightRatio = 7.0, darkRatio = 7.0)
            }
          },
        ),
        phone,
      )
    }

    onNodeWithText("1 part is hard to read").performScrollTo()
    onNodeWithText("Outlined in the preview").assertExists()
    onNodeWithText("Buttons and switches").assertExists()
    onNodeWithText("Light 1.5").assertExists()
  }

  @Test
  fun readability_saysSoWhenEveryCheckPasses() = runComposeUiTest {
    setContent {
      val state = rememberSampleThemeBuilderState()
      Builder(state.copy(contrast = state.contrast.map { it.copy(lightRatio = 7.0, darkRatio = 7.0) }), phone)
    }

    onNodeWithText("All 17 checks pass").performScrollTo()
    onNodeWithText("In light and dark mode").assertExists()
  }

  @Test
  fun save_asksForANameAndSaves() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNodeWithText("Save").performClick()
    onNodeWithText("Name your theme").assertExists()
    onNode(hasSetTextAction() and hasText("Custom theme 3")).assertExists()

    onNodeWithText("Save theme").performClick()
    assertEquals(ThemeBuilderUiEvent.Save, events.last())
  }

  @Test
  fun delete_confirmsBeforeDeleting() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent {
      Builder(rememberSampleThemeBuilderState(isNew = false, eventSink = { events += it }), phone)
    }

    onNodeWithText("Edit theme").assertExists()
    onNodeWithContentDescription("More options").performClick()
    onNodeWithText("Delete theme").performClick()
    onNodeWithText("Delete this theme?").assertExists()
    assertTrue(events.none { it == ThemeBuilderUiEvent.Delete })

    onNodeWithText("Delete").performClick()
    assertEquals(ThemeBuilderUiEvent.Delete, events.last())
  }

  @Test
  fun coverColorsNote_opensAppearanceSettings() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNodeWithText("The player uses cover colors").assertExists()
    onNodeWithText("Appearance settings").performClick()

    assertEquals(ThemeBuilderUiEvent.CoverThemingSettings, events.last())
  }

  @Test
  fun back_sendsBack() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNodeWithContentDescription("Back").performClick()

    assertEquals(ThemeBuilderUiEvent.Back, events.last())
  }
}
