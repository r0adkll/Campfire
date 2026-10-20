// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail.composables

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SeriesBookLabelTest {

  private fun assertLabel(expected: String, position: String?, year: String?, bookNumbered: Boolean = true) =
    runComposeUiTest {
      setContent {
        Text(seriesBookLabel(position = position, year = year, bookNumbered = bookNumbered) ?: "<none>")
      }
      onNodeWithText(expected).assertExists()
    }

  @Test
  fun plainNumbersReadAsBookNumbers() {
    assertLabel("Book 1.5 · 2021", position = "1.5", year = "2021")
    assertLabel("Book 3", position = "3", year = null)
  }

  @Test
  fun freeTextReadsAsSequence() {
    assertLabel("#prequel · 2021", position = "prequel", year = "2021")
    assertLabel("#2a", position = "2a", year = null)
  }

  @Test
  fun datedSeriesReadsEverySequenceAsSequence() {
    assertLabel("#2026.10.10 · 2008", position = "2026.10.10", year = "2008", bookNumbered = false)
    assertLabel("#2026", position = "2026", year = null, bookNumbered = false)
  }

  @Test
  fun unknownPartsAreLeftOut() {
    assertLabel("2021", position = " ", year = "2021")
    assertLabel("<none>", position = null, year = "")
  }
}
