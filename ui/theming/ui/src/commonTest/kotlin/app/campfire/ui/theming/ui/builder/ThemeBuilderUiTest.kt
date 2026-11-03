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
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
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
import androidx.compose.ui.unit.width
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

  /** An unfolded foldable held upright: the controls fit beside one preview. */
  private val foldable = Modifier.size(width = 700.dp, height = 840.dp)
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

  /** The sample state with every check passing except [check], which fails in light or dark. */
  @Composable
  private fun failing(check: ContrastCheck, dark: Boolean): ThemeBuilderUiState {
    val state = rememberSampleThemeBuilderState()
    return state.copy(
      contrast = state.contrast.map {
        when {
          it.check != check -> it.copy(lightRatio = 7.0, darkRatio = 7.0)
          dark -> it.copy(lightRatio = 7.0, darkRatio = 1.46)
          else -> it.copy(lightRatio = 1.46, darkRatio = 7.0)
        }
      },
    )
  }

  @Test
  fun phone_showsThePreviewOverATrayOfOneTool() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNodeWithText("New theme").assertExists()
    onNodeWithText("Search for your next story").assertExists()
    onNodeWithText("Tap to change").assertExists()
    onNode(hasText("Color") and isSelectable()).assertIsSelected()
    onNode(isToggleable() and hasText("Home")).assertExists()
    onNodeWithContentDescription("Preview in dark mode").assertExists()
    // The column of every control is for wider windows.
    onNodeWithText("Palette colors").assertDoesNotExist()
  }

  @Test
  fun toolbar_switchesWhatTheTrayShows() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNode(hasText("Style") and isSelectable()).performClick()
    onNodeWithText("Vibrant").assertExists()
    onNodeWithText("Tap to change").assertDoesNotExist()

    onNode(hasText("Palette") and isSelectable()).performClick()
    onNodeWithText("Tap a color to change it.").assertExists()

    onNode(hasText("Fine-tune") and isSelectable()).performClick()
    onNodeWithText("All colors").assertExists()
  }

  @Test
  fun expand_hidesTheTrayUntilAToolIsPicked() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNodeWithContentDescription("Show the whole preview").performClick()
    onNodeWithText("Tap to change").assertDoesNotExist()
    onNodeWithContentDescription("Show the controls").assertExists()

    onNode(hasText("Color") and isSelectable()).performClick()
    onNodeWithText("Tap to change").assertExists()
  }

  @Test
  fun styleCard_sendsTheStyle() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNode(hasText("Style") and isSelectable()).performClick()
    onNodeWithText("Vibrant").performScrollTo().performClick()

    assertEquals(ThemeBuilderUiEvent.StyleClick(Variant.VIBRANT), events.last())
  }

  @Test
  fun contrastLevel_sendsTheLevel() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNode(hasText("Style") and isSelectable()).performClick()
    onNodeWithText("High").performScrollTo().performClick()

    assertEquals(ThemeBuilderUiEvent.ContrastLevelClick(ContrastLevel.High), events.last())
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
  fun paletteTile_opensThePickerInTheTray() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNode(hasText("Palette") and isSelectable()).performClick()
    onNodeWithText("Supporting").performClick()
    onNodeWithText("Hue").assertExists()
    onNodeWithText("Colorfulness").assertExists()
    onNodeWithText("Search for your next story").assertExists()

    onNodeWithText("Done").performClick()
    onNodeWithText("Hue").assertDoesNotExist()
    onNodeWithText("Tap a color to change it.").assertExists()
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

    onNode(hasText("Palette") and isSelectable()).performClick()
    onNodeWithText("Supporting").performClick()
    onNodeWithText("Use generated").performScrollTo().performClick()

    assertEquals(ThemeBuilderUiEvent.ColorReset(PaletteKey.Supporting), events.last())
    onNodeWithText("Hue").assertDoesNotExist()
  }

  @Test
  fun readabilityChip_saysReadableWhenEveryCheckPasses() = runComposeUiTest {
    setContent {
      val state = rememberSampleThemeBuilderState()
      Builder(state.copy(contrast = state.contrast.map { it.copy(lightRatio = 7.0, darkRatio = 7.0) }), phone)
    }

    onNodeWithContentDescription("All 17 checks pass. In light and dark mode").assertExists()
  }

  @Test
  fun readabilityChip_opensThePartThatsHardToRead() = runComposeUiTest {
    setContent { Builder(failing(ContrastCheck.Buttons, dark = false), phone) }

    onNodeWithContentDescription("1 part is hard to read. Outlined in the preview").performClick()

    onNodeWithText("Buttons and switches").assertExists()
    onNodeWithText("Editing light mode").assertExists()
    onNode(hasText("Fine-tune") and isSelectable()).assertIsSelected()
    // Buttons only show in Everywhere, so the preview switches to it.
    onNode(isToggleable() and hasText("Everywhere")).assertIsOn()
  }

  @Test
  fun readabilityChip_opensThePartInTheModeItFails() = runComposeUiTest {
    setContent { Builder(failing(ContrastCheck.Buttons, dark = true), phone) }

    onNodeWithContentDescription("1 part is hard to read. Outlined in the preview").performClick()

    onNodeWithText("Editing dark mode").assertExists()
    onNodeWithContentDescription("Preview in dark mode").assertIsOn()
  }

  @Test
  fun previewPart_opensItsFineTuningInTheTray() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNodeWithText("Search for your next story").performClick()

    onNodeWithText("Search bar and dialog text").assertExists()
    onNodeWithText("Editing light mode").assertExists()
    onNodeWithText("Lightness").assertExists()
    onNodeWithText("Search for your next story").assertExists()
    onNode(hasText("Fine-tune") and isSelectable()).assertIsSelected()

    // Done goes back to the parts to fine-tune, not to the tool that was open before.
    onNodeWithText("Done").performClick()
    onNodeWithText("Lightness").assertDoesNotExist()
    onNodeWithText("All colors").assertExists()
  }

  @Test
  fun fineTuning_zoomsThePreviewOnThePart() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    // Offline status opens on Everywhere, zoomed in on the drawer's status chip.
    onNode(hasText("Fine-tune") and isSelectable()).performClick()
    onNodeWithText("Offline status").performScrollTo().performClick()
    waitForIdle()
    val status = onNodeWithText("Working offline", useUnmergedTree = true)
    val zoomed = status.getBoundsInRoot().width

    onNodeWithContentDescription("Show the whole screen").performClick()
    waitForIdle()
    val whole = status.getBoundsInRoot().width
    assertTrue(zoomed > whole, "The status is $zoomed wide zoomed in and $whole zoomed out")

    onNodeWithContentDescription("Zoom to this part").performClick()
    waitForIdle()
    assertEquals(zoomed, status.getBoundsInRoot().width)
  }

  @Test
  fun fineTune_setsTheChosenRoleInTheModeShown() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent { Builder(rememberSampleThemeBuilderState(eventSink = { events += it }), phone) }

    onNode(hasText("Fine-tune") and isSelectable()).performClick()
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

    onNode(hasText("Fine-tune") and isSelectable()).performClick()
    onNodeWithText("Book cards").performScrollTo().performClick()
    onNode(hasSetTextAction()).performTextReplacement("FFF3E0")
    waitForIdle()

    assertEquals(
      ThemeBuilderUiEvent.RoleColorPicked(ThemeRole.SurfaceContainerHigh, dark = false, color = Color(0xFFFFF3E0)),
      events.last(),
    )
  }

  @Test
  fun hardToReadPart_offersAFix() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent {
      Builder(rememberSampleThemeBuilderState(colors = unreadableCards, eventSink = { events += it }), phone)
    }

    onNode(hasText("Fine-tune") and isSelectable()).performClick()
    onNodeWithText("Book cards").performScrollTo().performClick()
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

    onNode(hasText("Fine-tune") and isSelectable()).performClick()
    onNodeWithText("Book cards").performScrollTo().performClick()
    onNodeWithText("Reset").performScrollTo().performClick()
    assertEquals(ThemeBuilderUiEvent.RolesReset(cardRoles, dark = false), events.last())

    onNodeWithText("Mirror to dark mode").performScrollTo().performClick()
    assertEquals(ThemeBuilderUiEvent.MirrorRoles(cardRoles, fromDark = false, ContrastCheck.CardText), events.last())
    onNodeWithText("Mirrored to dark mode").assertExists()
  }

  @Test
  fun fineTune_untunedPartHasNothingToResetOrMirror() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNode(hasText("Fine-tune") and isSelectable()).performClick()
    onNodeWithText("Account card").performScrollTo().performClick()

    onNodeWithText("Reset").performScrollTo().assertIsNotEnabled()
    onNodeWithText("Mirror to dark mode").assertIsNotEnabled()
    onNodeWithText("Only used here").assertExists()
    onNode(isToggleable() and hasText("Everywhere")).assertIsOn()
  }

  @Test
  fun allColors_tunesOneRoleOnItsOwn() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), phone) }

    onNode(hasText("Fine-tune") and isSelectable()).performClick()
    onNodeWithText("All colors").performScrollTo().performClick()
    onNodeWithText("surfaceContainerHigh").assertExists()
    onNodeWithText("primary").performScrollTo().performClick()

    onNode(hasText("Used by:", substring = true)).performScrollTo()
    onNodeWithText("Text and icons").assertDoesNotExist()

    // The tune panel's header stays put; the All colors list scrolls it away.
    onNodeWithText("Done").performClick()
    onNodeWithText("surfaceContainerHigh").assertExists()
    onNodeWithText("Done").performScrollTo().performClick()
    onNodeWithText("All colors").assertExists()
  }

  @Test
  fun foldable_showsTheControlsBesideOnePreview() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), foldable) }

    onNodeWithText("Palette colors").assertExists()
    onNodeWithText("Tap to change").assertExists()
    onNode(hasText("Palette") and isSelectable()).assertDoesNotExist()
    onNodeWithContentDescription("Preview in dark mode").assertExists()
    onNodeWithText("Light").assertDoesNotExist()
  }

  @Test
  fun foldable_paletteRowResetButtonClearsACustomPalette() = runComposeUiTest {
    val events = mutableListOf<ThemeBuilderUiEvent>()
    setContent {
      Builder(
        rememberSampleThemeBuilderState(
          colors = CustomThemeColors(Color(0xFFC0532D), Variant.EXPRESSIVE, errorColor = Color(0xFFD946EF)),
          eventSink = { events += it },
        ),
        foldable,
      )
    }

    onNodeWithContentDescription("Use the generated color").performScrollTo().performClick()

    assertEquals(ThemeBuilderUiEvent.ColorReset(PaletteKey.Errors), events.last())
  }

  @Test
  fun wide_showsLightAndDarkPreviewsWithoutAModeSwitch() = runComposeUiTest {
    setContent { Builder(rememberSampleThemeBuilderState(), wide) }

    onNodeWithText("Light").assertExists()
    onNodeWithText("Dark").assertExists()
    onNodeWithText("Palette colors").assertExists()
    onNodeWithContentDescription("Preview in dark mode").assertDoesNotExist()
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
