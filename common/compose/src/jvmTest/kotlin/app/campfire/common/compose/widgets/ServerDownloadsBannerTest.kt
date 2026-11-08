// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ServerDownloadsBannerTest {

  @Test
  fun `one episode reads in the singular`() = runComposeUiTest {
    setContent { ServerDownloadsBanner(count = 1, onClick = {}) }

    onNodeWithText("1 episode downloading on server").assertIsDisplayed()
  }

  @Test
  fun `several episodes read in the plural and open the queue`() = runComposeUiTest {
    var clicks = 0
    setContent { ServerDownloadsBanner(count = 4, onClick = { clicks++ }) }

    onNodeWithText("4 episodes downloading on server").performClick()

    assertThat(clicks).isEqualTo(1)
  }
}
