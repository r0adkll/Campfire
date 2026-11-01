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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
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
import app.campfire.ui.theming.api.RoleOverrides
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.CustomThemeColors
import app.campfire.ui.theming.ui.preview.previewBook
import com.r0adkll.swatchbuckler.color.dynamiccolor.Variant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ThemeBuilderUiTest {

  /** Card text fine-tuned to the card's own color in light mode, so Book cards fails there. */
  private val unreadableCards = CustomThemeColors(Color(0xFFC0532D), Variant.EXPRESSIVE).let { colors ->
    val card = colors.toGeneratedPalette().lightColorScheme.surfaceContainerHigh
    colors.copy(
      roleOverrides = RoleOverrides(
        light = mapOf(ThemeRole.SurfaceContainerHigh to card, ThemeRole.OnSurface to card),
      ),
    )
  }

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
  fun coverColor_picksThatBooksColor() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    val book = previewBook("li_piranesi", "Piranesi", "Susanna Clarke").item
    setContent {
      Builder(
        rememberSampleThemeBuilderState(
          coverColors = listOf(CoverColor(book, Color(0xFF2E5E4E))),
          eventSink = { events += it },
        ),
        phone,
      )
    }

    onNodeWithText("From your books").performScrollTo()
    onNodeWithContentDescription("Use the color of Piranesi").performClick()

    assertEquals(ThemeBuilderUiEvent.ColorPicked(PaletteKey.Accent, Color(0xFF2E5E4E)), events.last())
  }

  @Test
  fun coverColors_areHiddenUntilThereAreSome() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNodeWithText("Tap to change").assertExists()
    onNodeWithText("From your books").assertDoesNotExist()
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
    // The readability row comes first; the Fine-tune chip of the same name follows it.
    onAllNodesWithText("Buttons and switches")[0].assertExists()
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
  fun previewPart_opensItsFineTuningInPlaceOfTheControls() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNodeWithText("Continue Listening").performClick()

    onNodeWithText("Screen text").assertExists()
    onNodeWithText("Editing light mode").assertExists()
    onNodeWithText("Lightness").assertExists()
    onNodeWithText("Continue Listening").assertExists()

    onNodeWithText("Done").performClick()
    onNodeWithText("Lightness").assertDoesNotExist()
    onNodeWithText("Tap to change").assertExists()
  }

  @Test
  fun fineTune_setsTheChosenRoleInTheModeShown() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNodeWithText("Book cards").performScrollTo().performClick()
    onNodeWithText("Text and icons").performClick()
    onNodeWithContentDescription("Lightness").performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }

    val pick = assertIs<ThemeBuilderUiEvent.RoleColorPicked>(events.last())
    assertEquals(ThemeRole.OnSurface, pick.role)
    assertEquals(false, pick.dark)
  }

  @Test
  fun fineTune_typedHexSetsThatColor() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNodeWithText("Book cards").performScrollTo().performClick()
    onNode(hasSetTextAction()).performTextReplacement("FFF3E0")
    waitForIdle()

    assertEquals(
      ThemeBuilderUiEvent.RoleColorPicked(ThemeRole.SurfaceContainerHigh, dark = false, color = Color(0xFFFFF3E0)),
      events.last(),
    )
  }

  @Test
  fun readabilityRow_opensThePartWithAFix() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent {
      Builder(rememberSampleThemeBuilderState(colors = unreadableCards, eventSink = { events += it }), phone)
    }

    onAllNodesWithText("Book cards")[0].performScrollTo().performClick()
    onNodeWithText("Editing light mode").assertExists()
    onNodeWithText("Fix contrast").performClick()

    assertEquals(ThemeBuilderUiEvent.FixContrast(ContrastCheck.CardText, dark = false), events.last())
  }

  @Test
  fun fineTune_resetsAndMirrorsATunedPart() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent {
      Builder(rememberSampleThemeBuilderState(colors = unreadableCards, eventSink = { events += it }), phone)
    }
    val cardRoles = setOf(ThemeRole.SurfaceContainerHigh, ThemeRole.OnSurface)

    onAllNodesWithText("Book cards")[0].performScrollTo().performClick()
    onNodeWithText("Reset").performScrollTo().performClick()
    assertEquals(ThemeBuilderUiEvent.RolesReset(cardRoles, dark = false), events.last())

    onNodeWithText("Mirror to dark mode").performClick()
    assertEquals(ThemeBuilderUiEvent.MirrorRoles(cardRoles, fromDark = false, ContrastCheck.CardText), events.last())
    onNodeWithText("Mirrored to dark mode").assertExists()
  }

  @Test
  fun fineTune_untunedPartHasNothingToResetOrMirror() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNodeWithText("Account card").performScrollTo().performClick()

    onNodeWithText("Reset").performScrollTo().assertIsNotEnabled()
    onNodeWithText("Mirror to dark mode").assertIsNotEnabled()
    onNodeWithText("Only used here").assertExists()
  }

  @Test
  fun allColors_tunesOneRoleOnItsOwn() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNodeWithText("All colors").performScrollTo().performClick()
    onNodeWithText("surfaceContainerHigh").assertExists()
    onNodeWithText("primary").performClick()

    onNode(hasText("Used by:", substring = true)).performScrollTo()
    onNodeWithText("Text and icons").assertDoesNotExist()

    // The tune panel's header stays put; the All colors list scrolls it away.
    onNodeWithText("Done").performClick()
    onNodeWithText("surfaceContainerHigh").assertExists()
    onNodeWithText("Done").performScrollTo().performClick()
    onNodeWithText("Tap to change").assertExists()
  }

  @Test
  fun wide_tappingTheDarkPreviewTunesDarkMode() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), wide) }

    onAllNodesWithText("Continue Listening")[1].performClick()

    onNodeWithText("Editing dark mode").assertExists()
    onNodeWithContentDescription("Fine-tune dark mode").assertIsOn()
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
