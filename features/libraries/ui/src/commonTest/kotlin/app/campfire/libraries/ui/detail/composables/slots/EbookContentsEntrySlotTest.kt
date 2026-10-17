// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.reader.api.EbookContentsEntry
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class EbookContentsEntrySlotTest {

  @Test
  fun currentEntryShowsWhereItStartsAndOpensTheReaderThere() = runComposeUiTest {
    val events = mutableListOf<LibraryItemUiEvent>()
    val slot = EbookContentsEntrySlot(
      entry = EbookContentsEntry("Chapter 5", "ch5.xhtml", depth = 1, fraction = 0.667f),
      index = 4,
      isFirst = false,
      isLast = false,
      state = EbookContentsEntrySlot.State.Current,
      progressInEntry = 0.5f,
    )
    setContent { slot.Content(Modifier) { events += it } }

    onNodeWithText("67%").assertExists()
    onNodeWithText("Chapter 5").performClick()

    assertThat(events).containsExactly(LibraryItemUiEvent.ReadAt("ch5.xhtml"))
  }
}
