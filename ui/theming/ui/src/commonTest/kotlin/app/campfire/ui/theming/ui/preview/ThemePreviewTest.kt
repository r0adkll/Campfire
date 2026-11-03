// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import app.campfire.core.settings.ItemDisplayState
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.composables.label
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_tune_part
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalTestApi::class)
class ThemePreviewTest {

  private val previewSize = Modifier.size(width = 400.dp, height = 720.dp)

  /** The preview pane under the scene tabs on a 412dp-wide phone. */
  private val phonePane = Modifier.size(width = 380.dp, height = 330.dp)
  private val icon = AppTheme.Icon.Tent

  @Test
  fun home_showsTheUsersShelvesUnderTheSearchBar() = runComposeUiTest {
    setContent { ThemePreview(PreviewScene.Home, PreviewContent(previewLibrary()), icon, previewSize) }

    onNodeWithText("Search for your next story").assertExists()
    onNodeWithText("Continue Listening").assertExists()
    onNodeWithText("Project Hail Mary").assertExists()
    onNodeWithText("Recently Added").assertExists()
    onNodeWithText("Authors").assertExists()
  }

  @Test
  fun home_withoutTheUsersBooks_showsSamples() = runComposeUiTest {
    setContent { ThemePreview(PreviewScene.Home, PreviewContent(), icon, previewSize) }

    onNodeWithText("Continue Listening").assertExists()
    onNodeWithText("Moby-Dick").assertExists()
  }

  @Test
  fun home_whileThePlayerUsesCoverColors_saysSoInPlaceOfTheMiniPlayer() = runComposeUiTest {
    var opened = false
    setContent {
      ThemePreview(
        scene = PreviewScene.Home,
        content = PreviewContent(previewLibrary()),
        icon = icon,
        modifier = previewSize,
        onCoverThemingClick = { opened = true },
      )
    }

    onNodeWithText("The player uses cover colors").assertExists()
    onNodeWithText("5h 48m left").assertDoesNotExist()
    onNodeWithText("Appearance settings").performClick()
    assertTrue(opened)
  }

  @Test
  fun home_withPlayerCoverColorsOff_showsTheMiniPlayer() = runComposeUiTest {
    setContent {
      ThemePreview(
        PreviewScene.Home,
        PreviewContent(previewLibrary(), playerUsesCoverColors = false),
        icon,
        previewSize,
      )
    }

    onNodeWithText("The player uses cover colors").assertDoesNotExist()
    onNodeWithText("5h 48m left").assertExists()
  }

  @Test
  fun home_inAPodcastLibrary_showsPodcastTabs() = runComposeUiTest {
    setContent {
      ThemePreview(PreviewScene.Home, PreviewContent(previewLibrary(isPodcastLibrary = true)), icon, previewSize)
    }

    onNodeWithText("Shows").assertExists()
    onNodeWithText("Latest").assertExists()
    onNodeWithText("Authors").assertDoesNotExist()
  }

  @Test
  fun homeAndLibrary_drawAtTheSameScaleInAPhonesPane() = runComposeUiTest {
    val scene = mutableStateOf(PreviewScene.Home)
    setContent {
      ThemePreview(scene.value, PreviewContent(previewLibrary(), playerUsesCoverColors = false), icon, phonePane)
    }

    val homeTab = onNodeWithText("Series", useUnmergedTree = true).getBoundsInRoot()
    scene.value = PreviewScene.Library
    waitForIdle()
    val libraryTab = onNodeWithText("Series", useUnmergedTree = true).getBoundsInRoot()

    assertEquals(libraryTab.height, homeTab.height)
  }

  @Test
  fun home_inAPhonesPane_showsCoversRatherThanCutOffCards() = runComposeUiTest {
    setContent {
      ThemePreview(PreviewScene.Home, PreviewContent(previewLibrary(), playerUsesCoverColors = false), icon, phonePane)
    }

    onNodeWithContentDescription("Project Hail Mary").assertExists()
    onNodeWithText("Andy Weir").assertDoesNotExist()
    onNodeWithText("Chapter 5").assertExists()
  }

  @Test
  fun library_showsTheCountAndBooks() = runComposeUiTest {
    setContent { ThemePreview(PreviewScene.Library, PreviewContent(previewLibrary()), icon, previewSize) }

    onNodeWithText("65 Books").assertExists()
    onNodeWithText("Circe").assertExists()
  }

  @Test
  fun library_followsTheUsersListLayout() = runComposeUiTest {
    val library = previewLibrary(itemCount = null, isPodcastLibrary = true)
      .copy(displayState = ItemDisplayState.List)
    setContent { ThemePreview(PreviewScene.Library, PreviewContent(library), icon, previewSize) }

    onNodeWithText("5 Shows").assertExists()
    onNodeWithText("Dune").assertExists()
  }

  @Test
  fun everywhere_showsThePartsEveryScreenShares() = runComposeUiTest {
    setContent { ThemePreview(PreviewScene.Everywhere, PreviewContent(), icon, previewSize) }

    onNodeWithText("Working offline").assertExists()
    onNodeWithText("Download on Wi-Fi only").assertExists()
    onNodeWithText("New playlist").assertExists()
    onNodeWithText("You already have a playlist with this name").assertExists()
    onNodeWithText("Back online").assertExists()
  }

  @Test
  fun everySceneDrawsWithEveryCheckFlagged() = runComposeUiTest {
    val scene = mutableStateOf(PreviewScene.Home)
    setContent {
      ThemePreviewContent(
        scene = scene.value,
        content = PreviewContent(previewLibrary(), playerUsesCoverColors = false),
        icon = icon,
        targets = ThemeTargets(failing = ContrastCheck.entries.toSet()),
        modifier = previewSize,
      )
    }

    PreviewScene.entries.forEach { next ->
      scene.value = next
      waitForIdle()
    }
    onNodeWithText("Back online").assertExists()
  }

  @Test
  fun tappingAPart_reportsItsContrastCheck() = runComposeUiTest {
    var tapped: ContrastCheck? = null
    setContent {
      ThemePreviewContent(
        scene = PreviewScene.Everywhere,
        content = PreviewContent(),
        icon = icon,
        targets = ThemeTargets(onClick = { tapped = it }),
        modifier = previewSize,
      )
    }

    onNodeWithText("Working offline").performClick()
    assertEquals(ContrastCheck.OfflineStatus, tapped)

    onNodeWithText("Back online").performClick()
    assertEquals(ContrastCheck.Snackbars, tapped)
  }

  @Test
  fun tappingAPartInsideAnother_picksTheSmallerOne() = runComposeUiTest {
    var tapped: ContrastCheck? = null
    setContent {
      ThemePreviewContent(
        scene = PreviewScene.Home,
        content = PreviewContent(previewLibrary(), playerUsesCoverColors = false),
        icon = icon,
        targets = ThemeTargets(onClick = { tapped = it }),
        modifier = previewSize,
      )
    }

    // The selected tab's name sits inside the navigation bar.
    onNodeWithText("Home", useUnmergedTree = true).performClick()
    assertEquals(ContrastCheck.SelectedTabName, tapped)

    onNodeWithText("Library", useUnmergedTree = true).performClick()
    assertEquals(ContrastCheck.NavigationBar, tapped)
  }

  @Test
  fun parts_offerFineTuningToAccessibilityServices() = runComposeUiTest {
    var tapped: ContrastCheck? = null
    setContent {
      ThemePreviewContent(
        scene = PreviewScene.Everywhere,
        content = PreviewContent(),
        icon = icon,
        targets = ThemeTargets(onClick = { tapped = it }),
        modifier = previewSize,
      )
    }

    onNode(hasClickLabel("Fine-tune Snackbars and tooltips")).performSemanticsAction(SemanticsActions.OnClick)

    assertEquals(ContrastCheck.Snackbars, tapped)
  }

  @Test
  fun everyCheck_isDrawnByTheScenesItNames() = runComposeUiTest {
    val scene = mutableStateOf(PreviewScene.Home)
    lateinit var labels: Map<ContrastCheck, String>
    setContent {
      labels = ContrastCheck.entries.associateWith { stringResource(Res.string.theme_tune_part, it.label()) }
      ThemePreviewContent(
        scene = scene.value,
        content = PreviewContent(previewLibrary(), playerUsesCoverColors = false),
        icon = icon,
        targets = ThemeTargets(onClick = {}),
        modifier = previewSize,
      )
    }

    PreviewScene.entries.forEach { shown ->
      scene.value = shown
      waitForIdle()
      ContrastCheck.entries.filter { shown in it.scenes }.forEach { check ->
        assertTrue(
          // Unmerged: a part inside a control, like the selected tab's icon, merges into it.
          onAllNodes(hasClickLabel(labels.getValue(check)), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
          "$shown should draw $check",
        )
      }
    }
  }

  @Test
  fun withoutAClickHandler_partsAreNotClickable() = runComposeUiTest {
    setContent {
      ThemePreviewContent(
        scene = PreviewScene.Everywhere,
        content = PreviewContent(),
        icon = icon,
        targets = ThemeTargets.None,
        modifier = previewSize,
      )
    }

    onNodeWithText("Working offline").assert(hasClickAction().not())
  }

  @Test
  fun previewCard_switchesScenesAndReportsDarkMode() = runComposeUiTest {
    var darkMode = false
    setContent {
      ThemePreviewCard(
        isDarkMode = false,
        onDarkModeChange = { darkMode = it },
        content = PreviewContent(previewLibrary()),
        icon = icon,
      )
    }

    onNode(hasText("Library") and isToggleable()).performClick()
    onNodeWithText("65 Books").assertExists()

    onNodeWithText("Everywhere").performClick()
    onNodeWithText("Working offline").assertExists()

    onNodeWithContentDescription("Preview in dark mode").performClick()
    assertEquals(true, darkMode)
  }

  @Test
  fun previewCard_marksTheSelectedScene() = runComposeUiTest {
    setContent {
      ThemePreviewCard(isDarkMode = true, onDarkModeChange = {}, content = PreviewContent(), icon = icon)
    }

    onNode(hasText("Home") and isToggleable()).assertIsOn()
    onNode(hasText("Library") and isToggleable()).assertIsOff()
    onNode(hasContentDescription("Preview in dark mode") and isToggleable()).assertIsOn()
  }
}

private fun hasClickLabel(label: String) = SemanticsMatcher("has click label " + label) { node ->
  node.config.getOrNull(SemanticsActions.OnClick)?.label == label
}
