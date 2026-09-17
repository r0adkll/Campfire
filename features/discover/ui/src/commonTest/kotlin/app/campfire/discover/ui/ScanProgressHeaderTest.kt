// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ScanProgressHeaderTest {

  @Test
  fun loadingSeriesShowsAnIndeterminateBarAndWaitingText() = runComposeUiTest {
    setContent {
      ScanProgressHeader(done = 0, total = null, onCancel = {})
    }

    onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    onNodeWithText("Getting series from your server…").assertIsDisplayed()
    onNodeWithText("0 of 0 series").assertDoesNotExist()
  }

  @Test
  fun scanningShowsProgressAgainstTheTotal() = runComposeUiTest {
    setContent {
      ScanProgressHeader(done = 12, total = 87, onCancel = {})
    }

    onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(12f / 87, 0f..1f))).assertIsDisplayed()
    onNodeWithText("12 of 87 series").assertIsDisplayed()
    onNodeWithText("Getting series from your server…").assertDoesNotExist()
  }

  @Test
  fun cancelIsAvailableWhileLoadingSeries() = runComposeUiTest {
    var cancelled = 0
    setContent {
      ScanProgressHeader(done = 0, total = null, onCancel = { cancelled++ })
    }

    onNodeWithText("Cancel").performClick()

    assertEquals(1, cancelled)
  }
}
