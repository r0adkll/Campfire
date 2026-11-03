// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.session.LocalPlaybackBarHidden
import app.campfire.common.compose.session.LocalPlaybackSession
import app.campfire.common.compose.session.LocalPlayerDocked
import app.campfire.common.test.session
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CampfireWindowInsetsTest {

  /** Phone width, where the playback bar floats over the content. */
  private val phone = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(400f, 900f)

  @Test
  fun `while something plays, content insets for the floating bar`() {
    assertEquals(88, bottomInset(LocalPlaybackSession provides session()))
  }

  @Test
  fun `a screen that hides the bar doesn't reserve room for it`() {
    assertEquals(0, bottomInset(LocalPlaybackSession provides session(), LocalPlaybackBarHidden provides true))
  }

  @Test
  fun `a docked player doesn't need room either`() {
    assertEquals(0, bottomInset(LocalPlaybackSession provides session(), LocalPlayerDocked provides true))
  }

  @Test
  fun `with nothing playing there's no bar to inset for`() {
    assertEquals(0, bottomInset())
  }

  /** The bottom of [CampfireWindowInsets] in dp, at a density of 1 and with no system bars. */
  private fun bottomInset(vararg values: ProvidedValue<*>): Int {
    var bottom = -1
    runComposeUiTest {
      setContent {
        CompositionLocalProvider(
          LocalDensity provides Density(1f),
          LocalWindowSizeClass provides phone,
          LocalContentLayout provides ContentLayout.Root,
          *values,
        ) {
          bottom = CampfireWindowInsets.getBottom(LocalDensity.current)
        }
      }
      waitForIdle()
    }
    return bottom
  }
}
