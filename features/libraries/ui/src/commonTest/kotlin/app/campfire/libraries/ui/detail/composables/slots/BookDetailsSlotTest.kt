// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.home.ui.mediaMetadata
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.book_details_isbn
import campfire.features.libraries.ui.generated.resources.book_details_published
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class BookDetailsSlotTest {

  @Test
  fun from_keepsOnlyTheDetailsTheBookHas() {
    val slot = BookDetailsSlot.from(
      mediaMetadata(
        language = " ",
        ISBN = "978-3-16-148410-0",
        publisher = null,
        publishedDate = null,
        publishedYear = "2024",
      ),
    )

    assertThat(slot).isNotNull().prop(BookDetailsSlot::rows).containsExactly(
      Res.string.book_details_isbn to "978-3-16-148410-0",
      Res.string.book_details_published to "2024",
    )
  }

  @Test
  fun from_withoutAnyDetailsIsNull() {
    val slot = BookDetailsSlot.from(
      mediaMetadata(
        language = null,
        ISBN = null,
        publisher = null,
        publishedDate = null,
        publishedYear = null,
      ),
    )

    assertThat(slot).isNull()
  }

  @Test
  fun contentTest() = runComposeUiTest {
    val slot = BookDetailsSlot.from(mediaMetadata())!!

    setContent {
      slot.Content(Modifier) {}
    }

    onNodeWithText("Details").assertExists()
    onNodeWithText("en-US").assertExists()
    onNodeWithText("978-3-16-148410-0").assertExists()
    onNodeWithText("Fake House Publishing").assertExists()
    onNodeWithText("2024-10-26").assertExists()
  }
}
