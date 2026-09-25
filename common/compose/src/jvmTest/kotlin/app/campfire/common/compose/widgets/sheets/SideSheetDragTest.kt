// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.sheets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.theme.CampfireTheme
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import kotlin.test.Test

/**
 * The panel is closed by dragging it back towards the edge it came from, which is what its handle
 * advertises. This drives the gesture rather than the geometry, because the two can disagree: a
 * tap-blocker on the panel that sits closer to the content than the drag modifier consumes the
 * pointer down first, and the drag then never starts, however correct the layout looks.
 */
@OptIn(ExperimentalTestApi::class)
class SideSheetDragTest {

  @Test
  fun `dragging the panel towards its edge closes it`() = runComposeUiTest {
    var dismissed = false
    showPanel { dismissed = true }

    onNodeWithTag(PanelBody).performTouchInput { swipeRight() }
    waitForIdle()

    assertThat(dismissed).isTrue()
  }

  @Test
  fun `dragging it the other way leaves it open`() = runComposeUiTest {
    var dismissed = false
    showPanel { dismissed = true }

    onNodeWithTag(PanelBody).performTouchInput { swipeLeft() }
    waitForIdle()

    assertThat(dismissed).isFalse()
  }

  /**
   * The panel has to follow the finger. Dismissing on release is not enough on its own — a panel
   * that does not move while being dragged reads as not draggable at all, whatever happens when
   * you let go.
   */
  @Test
  fun `the panel follows the finger while being dragged`() = runComposeUiTest {
    showPanel { }

    val atRest = onNodeWithTag(PanelBody).getBoundsInRoot().left

    onNodeWithTag(PanelBody).performTouchInput {
      down(center)
      moveBy(Offset(120f, 0f))
    }
    waitForIdle()

    val whileDragging = onNodeWithTag(PanelBody).getBoundsInRoot().left
    assertThat(whileDragging.value).isGreaterThan(atRest.value)
  }

  @Test
  fun `letting go short of the threshold springs it back`() = runComposeUiTest {
    var dismissed = false
    showPanel { dismissed = true }

    val atRest = onNodeWithTag(PanelBody).getBoundsInRoot().left

    onNodeWithTag(PanelBody).performTouchInput {
      down(center)
      moveBy(Offset(24f, 0f))
      up()
    }

    // The spring settles over several frames, so wait for the panel to arrive rather than
    // assuming one idle pass is enough; a panel that never returns fails on the timeout.
    waitUntil(timeoutMillis = 2_000) {
      onNodeWithTag(PanelBody).getBoundsInRoot().left.value == atRest.value
    }

    assertThat(dismissed).isFalse()
  }

  /**
   * What the sheets actually put in the panel: a scrolling list. It claims the vertical axis, and
   * a drag across it still has to reach the panel underneath.
   */
  @Test
  fun `dragging across a scrolling list still closes it`() = runComposeUiTest {
    var dismissed = false
    showPanel(scrollingBody = true) { dismissed = true }

    onNodeWithTag(PanelBody).performTouchInput { swipeRight() }
    waitForIdle()

    assertThat(dismissed).isTrue()
  }

  private fun ComposeUiTest.showPanel(scrollingBody: Boolean = false, onDismiss: () -> Unit) {
    setContent {
      // Wide and short, so the policy picks the side panel.
      val sizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(914f, 411f)

      CompositionLocalProvider(LocalWindowSizeClass provides sizeClass) {
        CampfireTheme(useDarkColors = false) {
          val overlayHost = rememberOverlayHost()
          ContentWithOverlays(overlayHost = overlayHost, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize())
          }

          LaunchedEffect(Unit) {
            overlayHost.show(
              AdaptiveSheetOverlay<Unit, Unit>(
                model = Unit,
                onDismiss = onDismiss,
              ) { _, _ -> PanelBodyContent(scrollingBody) },
            )
          }
        }
      }
    }
    waitForIdle()
  }

  @Composable
  private fun PanelBodyContent(scrolling: Boolean) {
    if (scrolling) {
      LazyColumn(Modifier.fillMaxSize().testTag(PanelBody)) {
        items(30) { index -> Box(Modifier.fillMaxWidth().height(48.dp)) }
      }
    } else {
      Box(Modifier.fillMaxSize().testTag(PanelBody))
    }
  }

  private companion object {
    const val PanelBody = "panel-body"
  }
}
