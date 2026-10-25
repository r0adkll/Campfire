// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import app.campfire.core.model.SeriesSequence
import app.campfire.home.ui.authorMetadata
import app.campfire.home.ui.mediaMetadata
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.SeriesEntry
import app.campfire.management.api.match.BookMatch
import app.campfire.management.ui.editor.BookDetail
import app.campfire.management.ui.editor.BookDetailsDraft
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

/** What a match would change of a book, what's chosen of it, and how it fills the form. */
class MatchReviewTest {

  private val details = mediaMetadata(
    title = "Children of Ruin",
    subtitle = null,
    description = "<p>An expedition follows <b>an old signal</b>.</p>",
    publisher = "Pan Macmillan",
    publishedYear = "2019",
    ISBN = null,
    ASIN = "B07NDMXD3K",
    language = null,
    isExplicit = false,
    isAbridged = false,
    authors = listOf(authorMetadata(id = "author-1", name = "Adrian Tchaikovsky")),
    narrators = listOf("Mel Hudson"),
    seriesSequence = SeriesSequence(id = "series-1", name = "Children of Time", sequence = 2.0, label = "2"),
    genres = listOf("Science Fiction"),
  )
  private val tags = listOf("Book club")
  private val draft = BookDetailsDraft(details, tags)

  @Test
  fun `a review has a row for each detail the match has, beside the book's`() {
    val match = BookMatch(
      title = "Children of Ruin",
      subtitle = "Children of Time, Book 2",
      authors = listOf("Adrian Tchaikovsky"),
      coverUrl = NewCover,
      isbn = "9781509865857",
    )

    val rows = reviewRows(match, draft, BookCover)

    assertThat(rows.map { it.field }).containsExactly(
      MatchField.Cover,
      MatchField.Title,
      MatchField.Subtitle,
      MatchField.Authors,
      MatchField.Isbn,
    )
    assertThat(rows.row(MatchField.Cover).current).isEqualTo(MatchValue.Image(BookCover))
    assertThat(rows.row(MatchField.Title).isSame).isTrue()
    assertThat(rows.row(MatchField.Subtitle).current).isNull()
    assertThat(rows.row(MatchField.Isbn).isSame).isFalse()
  }

  @Test
  fun `authors and series in another case or order are the same`() {
    val match = BookMatch(
      authors = listOf("adrian tchaikovsky"),
      series = listOf(SeriesEntry("children of time", "2")),
      narrators = listOf("mel hudson"),
    )

    val rows = reviewRows(match, draft, BookCover)

    assertThat(rows.row(MatchField.Authors).isSame).isTrue()
    assertThat(rows.row(MatchField.Series).isSame).isTrue()
    // A narrator's name is shown as the source spells it
    assertThat(rows.row(MatchField.Narrators).isSame).isFalse()
  }

  @Test
  fun `a description is the same in other markup`() {
    val match = BookMatch(description = "An expedition follows <i>an old signal</i>.")

    assertThat(reviewRows(match, draft, BookCover).row(MatchField.Description).isSame).isTrue()
  }

  @Test
  fun `a cover is never the same, and is only taken with the upload permission`() {
    val rows = reviewRows(BookMatch(coverUrl = BookCover), draft, BookCover)

    assertThat(rows.single().isSame).isFalse()
    assertThat(rows.selectedBy(SelectionMode.All, canChangeCover = false)).isEmpty()
    assertThat(rows.selectedBy(SelectionMode.All, canChangeCover = true)).containsOnly(MatchField.Cover)
  }

  @Test
  fun `each way of choosing takes only what would change`() {
    val match = BookMatch(
      title = "Children of Ruin",
      subtitle = "Children of Time, Book 2",
      publisher = "Orbit",
      explicit = false,
      abridged = true,
    )
    val rows = reviewRows(match, draft, BookCover)

    assertThat(rows.selectedBy(SelectionMode.All, canChangeCover = true))
      .containsOnly(MatchField.Subtitle, MatchField.Publisher, MatchField.Abridged)
    assertThat(rows.selectedBy(SelectionMode.EmptyOnly, canChangeCover = true)).containsOnly(MatchField.Subtitle)
    assertThat(rows.selectedBy(SelectionMode.None, canChangeCover = true)).isEmpty()
  }

  @Test
  fun `filling the form replaces the chosen lists, or adds to genres and tags`() {
    val match = BookMatch(
      authors = listOf("Adrian Tchaikovsky", "Someone Else"),
      narrators = listOf("Not Chosen"),
      series = listOf(SeriesEntry("Children of Time", "2"), SeriesEntry("Tchaikovsky Universe")),
      genres = listOf("Space Opera", "science fiction"),
      tags = listOf("Audible"),
    )

    draft.fillFrom(
      match = match,
      fields = setOf(MatchField.Authors, MatchField.Series, MatchField.Genres, MatchField.Tags),
      merges = mapOf(MatchField.Genres to ListMerge.Add),
    )

    assertThat(draft.update()).isEqualTo(
      BookDetailsUpdate(
        authors = listOf("Adrian Tchaikovsky", "Someone Else"),
        series = listOf(SeriesEntry("Children of Time", "2"), SeriesEntry("Tchaikovsky Universe")),
        genres = listOf("Science Fiction", "Space Opera"),
        tags = listOf("Audible"),
      ),
    )
  }

  @Test
  fun `filling the form sets the chosen details and asks for the new cover`() {
    val match = BookMatch(
      title = "Children of Ruin: A Novel",
      publishedYear = "2020",
      description = "<p>New <b>words</b></p>",
      coverUrl = NewCover,
      explicit = true,
      publisher = "Not Chosen",
    )

    draft.fillFrom(
      match = match,
      fields = setOf(
        MatchField.Title,
        MatchField.PublishedYear,
        MatchField.Description,
        MatchField.Cover,
        MatchField.Explicit,
      ),
    )

    assertThat(draft.changes).containsOnly(
      BookDetail.Cover,
      BookDetail.Title,
      BookDetail.PublishedYear,
      BookDetail.Description,
      BookDetail.Explicit,
    )
    assertThat(draft.description.annotatedString.text).isEqualTo("New words")
    assertThat(draft.newCoverUrl).isEqualTo(NewCover)
    assertThat(draft.update().coverUrl).isEqualTo(NewCover)
    assertThat(draft.update().publisher).isNull()
  }

  @Test
  fun `a description's text is compared without its markup`() {
    assertThat("<p>One&nbsp;&amp; two</p><p>Three<br/>four</p>".plain()).isEqualTo("One & two Three four")
  }

  private fun List<MatchRow>.row(field: MatchField) = first { it.field == field }

  private companion object {
    const val BookCover = "https://abs.example/api/items/book/cover"
    const val NewCover = "https://m.media-amazon.com/images/I/cover.jpg"
  }
}
