// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.campfire.core.model.SeriesSequence
import app.campfire.home.ui.authorMetadata
import app.campfire.home.ui.mediaMetadata
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.SeriesEntry
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
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
    authors = listOf(authorMetadata(id = "author-1", name = "Adrian Tchaikovsky")),
    narrators = listOf("Mel Hudson"),
    seriesSequence = SeriesSequence(id = "series-1", name = "Children of Time", sequence = 2.0, label = "2"),
    genres = listOf("Science Fiction"),
  )
  private val tags = listOf("Book club")

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
  fun `untouched lists change nothing`() {
    val draft = BookDetailsDraft(details, tags)

    assertThat(draft.authors.tokens).containsExactly("Adrian Tchaikovsky")
    assertThat(draft.series.map { it.name.text.toString() to it.sequence.text.toString() })
      .containsExactly("Children of Time" to "2")
    assertThat(draft.tags.tokens).containsExactly("Book club")
    assertThat(draft.changes).isEmpty()
  }

  @Test
  fun `a changed list is sent whole`() {
    val draft = BookDetailsDraft(details, tags)

    draft.narrators.add("Gary Furlong")
    draft.genres.remove("Science Fiction")
    draft.tags.add("Favourites")

    assertThat(draft.changes)
      .containsExactlyInAnyOrder(BookDetail.Narrators, BookDetail.Genres, BookDetail.Tags)
    assertThat(draft.update()).isEqualTo(
      BookDetailsUpdate(
        narrators = listOf("Mel Hudson", "Gary Furlong"),
        genres = emptyList(),
        tags = listOf("Book club", "Favourites"),
      ),
    )
  }

  @Test
  fun `a name typed but not yet added is kept`() {
    val draft = BookDetailsDraft(details, tags)

    draft.genres.input.setTextAndPlaceCursorAtEnd(" Space Opera ")

    assertThat(draft.update()).isEqualTo(BookDetailsUpdate(genres = listOf("Science Fiction", "Space Opera")))
  }

  @Test
  fun `a name isn't added twice, ignoring case`() {
    val draft = BookDetailsDraft(details, tags)

    assertThat(draft.genres.add("science fiction")).isFalse()
    draft.genres.input.setTextAndPlaceCursorAtEnd("SCIENCE FICTION")

    assertThat(draft.changes).isEmpty()
  }

  @Test
  fun `authors are the same whatever their order or capitals, as on the server`() {
    val draft = BookDetailsDraft(details, tags)

    draft.authors.remove("Adrian Tchaikovsky")
    draft.authors.add("adrian tchaikovsky")

    // Typed back, it keeps the capitals it had
    assertThat(draft.authors.tokens).containsExactly("Adrian Tchaikovsky")
    assertThat(draft.changes).isEmpty()

    draft.authors.add("Ann Leckie")
    assertThat(draft.update()).isEqualTo(BookDetailsUpdate(authors = listOf("Adrian Tchaikovsky", "Ann Leckie")))
  }

  @Test
  fun `the last name can be taken back to change it`() {
    val draft = BookDetailsDraft(details, tags)

    assertThat(draft.narrators.editLast()).isTrue()

    assertThat(draft.narrators.tokens).isEmpty()
    assertThat(draft.narrators.input.text.toString()).isEqualTo("Mel Hudson")
    // Still there, as it's typed
    assertThat(draft.changes).isEmpty()
    // Not while something's typed
    assertThat(draft.narrators.editLast()).isFalse()
  }

  @Test
  fun `series are sent with their numbers as text, a missing one as none`() {
    val draft = BookDetailsDraft(details, tags)

    draft.series.single().sequence.setTextAndPlaceCursorAtEnd("2.5")
    draft.addSeries()
    draft.series.last().name.setTextAndPlaceCursorAtEnd(" The Final Architecture ")

    assertThat(draft.changes).containsOnly(BookDetail.Series)
    assertThat(draft.update()).isEqualTo(
      BookDetailsUpdate(
        series = listOf(
          SeriesEntry("Children of Time", "2.5"),
          SeriesEntry("The Final Architecture", null),
        ),
      ),
    )
  }

  @Test
  fun `a series row left empty is no change, and removing one is`() {
    val draft = BookDetailsDraft(details, tags)

    draft.addSeries()
    assertThat(draft.changes).isEmpty()
    assertThat(draft.isValid).isTrue()

    draft.removeSeries(draft.series.first())
    assertThat(draft.update()).isEqualTo(BookDetailsUpdate(series = emptyList()))
  }

  @Test
  fun `a series is the same whatever its capitals, as on the server`() {
    val draft = BookDetailsDraft(details, tags)

    draft.series.single().name.setTextAndPlaceCursorAtEnd("children of time")

    assertThat(draft.changes).isEmpty()
  }

  @Test
  fun `a series needs a name, once, to be saved`() {
    val draft = BookDetailsDraft(details, tags)

    draft.addSeries()
    draft.series.last().sequence.setTextAndPlaceCursorAtEnd("3")
    assertThat(draft.seriesErrorAt(1)).isEqualTo(SeriesRowError.MissingName)
    assertThat(draft.isValid).isFalse()

    draft.series.last().name.setTextAndPlaceCursorAtEnd("Children Of Time")
    assertThat(draft.seriesErrorAt(0)).isEqualTo(null)
    assertThat(draft.seriesErrorAt(1)).isEqualTo(SeriesRowError.Duplicate)
    assertThat(draft.isValid).isFalse()
  }

  @Test
  fun `undoing puts back the lists as they were`() {
    val draft = BookDetailsDraft(details, tags)
    draft.authors.add("Ann Leckie")
    draft.series.single().sequence.setTextAndPlaceCursorAtEnd("")
    draft.tags.remove("Book club")

    val undo = draft.undoOf(draft.update())

    assertThat(undo).isEqualTo(
      BookDetailsUpdate(
        authors = listOf("Adrian Tchaikovsky"),
        series = listOf(SeriesEntry("Children of Time", "2")),
        tags = listOf("Book club"),
      ),
    )
  }

  @Test
  fun `a draft knows when the server's details moved on`() {
    val draft = BookDetailsDraft(details, tags)

    assertThat(draft.isBehind(details, tags)).isFalse()
    assertThat(draft.isBehind(details.copy(publisher = "Orbit"), tags)).isTrue()
    assertThat(draft.isBehind(details.copy(narrators = listOf("Gary Furlong")), tags)).isTrue()
    assertThat(draft.isBehind(details, listOf("Book club", "Favourites"))).isTrue()
    // Details the editor doesn't change don't count
    assertThat(draft.isBehind(details.copy(narratorName = "Someone else"), tags)).isFalse()
  }
}
