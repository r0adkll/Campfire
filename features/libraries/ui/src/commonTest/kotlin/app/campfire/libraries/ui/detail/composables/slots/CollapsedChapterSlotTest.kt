// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CollapsedChapterSlotTest {

  @Test
  fun listenedChaptersTest() = runComposeUiTest {
    setContent { CollapsedChapterSlot(3).Content(Modifier) {} }

    onNodeWithText("3 listened chapters").assertExists()
  }

  @Test
  fun readChaptersExpandOnClickTest() = runComposeUiTest {
    val events = mutableListOf<LibraryItemUiEvent>()
    setContent { CollapsedChapterSlot(1, isRead = true).Content(Modifier) { events += it } }

    onNodeWithText("1 read chapter").performClick()

    assertThat(events).containsExactly(LibraryItemUiEvent.ExpandChaptersClick)
  }
}
