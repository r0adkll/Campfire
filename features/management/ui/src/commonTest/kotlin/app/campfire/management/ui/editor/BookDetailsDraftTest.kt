// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.campfire.home.ui.mediaMetadata
import app.campfire.libraries.api.BookDetailsUpdate
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

/** What an edited book's details send the server, and what puts them back. */
class BookDetailsDraftTest {

  private val details = mediaMetadata(
    title = "Children of Ruin",
    subtitle = null,
    description = "<p>An expedition follows <b>an old signal</b>.</p>",
    publisher = "Pan Macmillan",
    publishedYear = "2019",
    isExplicit = false,
  )

  @Test
  fun `untouched details change nothing`() {
    val draft = BookDetailsDraft(details)

    assertThat(draft.changes).isEmpty()
    assertThat(draft.update().isEmpty).isTrue()
  }

  @Test
  fun `only the changed details are sent, trimmed`() {
    val draft = BookDetailsDraft(details)

    draft.title.setTextAndPlaceCursorAtEnd("  Children of Memory ")
    draft.explicit = true

    assertThat(draft.changes).containsExactlyInAnyOrder(BookDetail.Title, BookDetail.Explicit)
    assertThat(draft.update()).isEqualTo(BookDetailsUpdate(title = "Children of Memory", explicit = true))
  }

  @Test
  fun `spaces alone aren't a change`() {
    val draft = BookDetailsDraft(details)

    draft.publisher.setTextAndPlaceCursorAtEnd("Pan Macmillan  ")

    assertThat(draft.changes).isEmpty()
  }

  @Test
  fun `a cleared detail is sent as empty text`() {
    val draft = BookDetailsDraft(details)

    draft.publisher.setTextAndPlaceCursorAtEnd("")

    assertThat(draft.update()).isEqualTo(BookDetailsUpdate(publisher = ""))
  }

  @Test
  fun `a detail filled in for the first time is a change`() {
    val draft = BookDetailsDraft(details)

    draft.subtitle.setTextAndPlaceCursorAtEnd("Children of Time 2")

    assertThat(draft.update()).isEqualTo(BookDetailsUpdate(subtitle = "Children of Time 2"))
  }

  @Test
  fun `a rewritten description is sent with its formatting`() {
    val draft = BookDetailsDraft(details)

    draft.description.setHtml("<p>A <b>new</b> description</p>")

    assertThat(draft.changes).containsExactlyInAnyOrder(BookDetail.Description)
    val sent = draft.update().description.orEmpty()
    assertThat(sent.contains("<b>new</b>")).isTrue()
  }

  @Test
  fun `a cleared description is sent as empty text`() {
    val draft = BookDetailsDraft(details)

    draft.description.setHtml("")

    assertThat(draft.update()).isEqualTo(BookDetailsUpdate(description = ""))
  }

  @Test
  fun `a book needs a title`() {
    val draft = BookDetailsDraft(details)

    draft.title.setTextAndPlaceCursorAtEnd("   ")

    assertThat(draft.isTitleMissing).isTrue()
    assertThat(draft.isValid).isFalse()
  }

  @Test
  fun `a year is four digits or nothing`() {
    val draft = BookDetailsDraft(details)

    draft.publishedYear.setTextAndPlaceCursorAtEnd("19")
    assertThat(draft.isPublishedYearInvalid).isTrue()

    draft.publishedYear.setTextAndPlaceCursorAtEnd("")
    assertThat(draft.isPublishedYearInvalid).isFalse()

    draft.publishedYear.setTextAndPlaceCursorAtEnd("2020")
    assertThat(draft.isValid).isTrue()
  }

  @Test
  fun `undoing puts back what the changed details were, and only those`() {
    val draft = BookDetailsDraft(details)
    draft.title.setTextAndPlaceCursorAtEnd("Children of Memory")
    draft.subtitle.setTextAndPlaceCursorAtEnd("A subtitle")
    draft.description.setHtml("<p>Something else</p>")

    val undo = draft.undoOf(draft.update())

    assertThat(undo).isEqualTo(
      BookDetailsUpdate(
        title = "Children of Ruin",
        // It had none, which empty text puts back
        subtitle = "",
        // Exactly as the server had it, not as the editor reads it
        description = "<p>An expedition follows <b>an old signal</b>.</p>",
      ),
    )
  }

  @Test
  fun `a draft knows when the server's details moved on`() {
    val draft = BookDetailsDraft(details)

    assertThat(draft.isBehind(details)).isFalse()
    assertThat(draft.isBehind(details.copy(publisher = "Orbit"))).isTrue()
    // Details the editor doesn't change don't count
    assertThat(draft.isBehind(details.copy(narratorName = "Someone else"))).isFalse()
  }
}
