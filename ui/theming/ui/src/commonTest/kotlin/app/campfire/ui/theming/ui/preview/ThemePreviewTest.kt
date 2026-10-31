// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ThemePreviewTest {

  private val previewSize = Modifier.size(width = 360.dp, height = 640.dp)

  @Test
  fun homeScene_showsShelfMiniPlayerAndNavigation() = runComposeUiTest {
    setContent { ThemePreview(PreviewScene.Home, previewSize) }

    onNodeWithText("Continue listening").assertExists()
    onNodeWithText("Moby-Dick").assertExists()
    onNodeWithText("3 new").assertExists()
    onNodeWithText("Chapter 5 · 12 min left").assertExists()
    onNodeWithText("Stats").assertExists()
  }

  @Test
  fun bookScene_highlightsTheChapterBeingPlayed() = runComposeUiTest {
    setContent { ThemePreview(PreviewScene.Book, previewSize) }

    onNodeWithText("Jane Austen · read by Karen Savage").assertExists()
    onNodeWithText("Continue").assertExists()
    onNodeWithText("Chapter 13 · Playing").assertExists()
    onNodeWithText("Chapter 12").assertExists()
  }

  @Test
  fun playerScene_showsControlsAndChapter() = runComposeUiTest {
    setContent { ThemePreview(PreviewScene.Player, previewSize) }

    onNodeWithText("The Time Machine").assertExists()
    onNodeWithText("Chapter 4 of 12").assertExists()
    onNodeWithText("Sleep in 15 min").assertExists()
  }

  @Test
  fun edgeCasesScene_showsTheHardToReadParts() = runComposeUiTest {
    setContent { ThemePreview(PreviewScene.EdgeCases, previewSize) }

    onNodeWithText("Remove download?").assertExists()
    onNodeWithText("Can't reach your server. Playing downloads only.").assertExists()
    onNodeWithText("Can't connect to this address").assertExists()
    onNodeWithText("Finished").assertExists()
    onNodeWithText("Marked as finished").assertExists()
  }

  @Test
  fun everySceneDrawsWithEveryCheckFlagged() = runComposeUiTest {
    val scene = mutableStateOf(PreviewScene.Home)
    setContent {
      ThemePreviewContent(
        scene = scene.value,
        targets = ThemeTargets(failing = ContrastCheck.entries.toSet()),
        modifier = previewSize,
      )
    }

    PreviewScene.entries.forEach { next ->
      scene.value = next
      waitForIdle()
    }
    onNodeWithText("Marked as finished").assertExists()
  }

  @Test
  fun tappingAPart_reportsItsContrastCheck() = runComposeUiTest {
    var tapped: ContrastCheck? = null
    setContent {
      ThemePreviewContent(
        scene = PreviewScene.EdgeCases,
        targets = ThemeTargets(onClick = { tapped = it }),
        modifier = previewSize,
      )
    }

    onNodeWithText("Undo").performScrollTo().performClick()
    assertEquals(ContrastCheck.SnackbarActions, tapped)

    onNodeWithText("Can't reach your server. Playing downloads only.").performScrollTo().performClick()
    assertEquals(ContrastCheck.ErrorBanners, tapped)
  }

  @Test
  fun withoutAClickHandler_partsAreNotClickable() = runComposeUiTest {
    setContent {
      ThemePreviewContent(scene = PreviewScene.Home, targets = ThemeTargets.None, modifier = previewSize)
    }

    onNodeWithText("See all").assert(hasClickAction().not())
  }

  @Test
  fun previewCard_switchesScenesAndReportsDarkMode() = runComposeUiTest {
    var darkMode = false
    setContent {
      ThemePreviewCard(isDarkMode = false, onDarkModeChange = { darkMode = it })
    }

    onNodeWithText("Book").performClick()
    onNodeWithText("Chapter 13 · Playing").assertExists()

    onNodeWithText("Edge cases").performClick()
    onNodeWithText("Remove download?").assertExists()

    onNodeWithContentDescription("Preview in dark mode").performClick()
    assertEquals(true, darkMode)
  }

  @Test
  fun previewCard_marksTheSelectedScene() = runComposeUiTest {
    setContent { ThemePreviewCard(isDarkMode = true, onDarkModeChange = {}) }

    onNode(hasText("Home") and isToggleable()).assertIsOn()
    onNode(hasText("Book") and isToggleable()).assertIsOff()
    onNode(hasContentDescription("Preview in dark mode") and isToggleable()).assertIsOn()
  }
}
