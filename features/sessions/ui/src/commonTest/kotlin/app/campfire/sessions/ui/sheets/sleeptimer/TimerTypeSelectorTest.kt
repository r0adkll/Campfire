// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.sleeptimer

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import app.campfire.common.compose.theme.CampfireTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TimerTypeSelectorTest {

  @Test
  fun `both timer type buttons share a height when a label wraps`() = runComposeUiTest {
    setContent {
      CampfireTheme {
        TimerTypeSelector(
          isEpochTimeSelection = true,
          onTimerTypeChange = {},
          modifier = Modifier.width(220.dp),
        )
      }
    }

    // The merged tree folds each label into its toggle button, so these are the buttons' bounds
    val timeButton = onNodeWithText("Time").getBoundsInRoot()
    val chapterButton = onNodeWithText("End of Chapter").getBoundsInRoot()

    val chapterText = onNodeWithText("End of Chapter", useUnmergedTree = true).getBoundsInRoot()
    val timeText = onNodeWithText("Time", useUnmergedTree = true).getBoundsInRoot()
    assertThat(chapterText.height).isGreaterThan(timeText.height)

    assertThat(timeButton.height).isEqualTo(chapterButton.height)
  }
}
