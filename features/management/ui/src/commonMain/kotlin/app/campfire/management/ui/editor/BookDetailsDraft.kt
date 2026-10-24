// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.campfire.common.compose.extensions.toRichTextHtml
import app.campfire.core.model.Media
import app.campfire.libraries.api.BookDetailsUpdate
import com.mohamedrejeb.richeditor.model.RichTextState

/** A detail of a book that the editor changes. */
enum class BookDetail {
  Title,
  Subtitle,
  Description,
  Publisher,
  PublishedYear,
  Language,
  Isbn,
  Asin,
  Explicit,
  Abridged,
}

/**
 * A book's details as they're being edited, from [base], the details as the server had them when
 * editing started. Each field is edited in place; what differs from [base] is what a save sends.
 */
@Stable
class BookDetailsDraft(val base: Media.Metadata) {

  val title = TextFieldState(base.title.orEmpty())
  val subtitle = TextFieldState(base.subtitle.orEmpty())
  val publisher = TextFieldState(base.publisher.orEmpty())
  val publishedYear = TextFieldState(base.publishedYear.orEmpty())
  val language = TextFieldState(base.language.orEmpty())
  val isbn = TextFieldState(base.ISBN.orEmpty())
  val asin = TextFieldState(base.ASIN.orEmpty())
  var explicit by mutableStateOf(base.isExplicit)
  var abridged by mutableStateOf(base.isAbridged)

  /** Edited with its formatting, which the server keeps as HTML */
  val description = RichTextState().apply {
    setHtml(base.description.orEmpty().toRichTextHtml(linkify = false))
  }

  // What the editor makes of the description as it came, which an untouched one still matches
  private val baseDescriptionHtml = description.toHtml()

  /** The details that differ from [base]. */
  val changes: Set<BookDetail>
    get() = BookDetail.entries.filterTo(mutableSetOf()) { isChanged(it) }

  val isTitleMissing: Boolean
    get() = title.text.toString().isBlank()

  /** A year the server would take: four digits, or nothing */
  val isPublishedYearInvalid: Boolean
    get() = publishedYear.text.toString().trim().let { it.isNotEmpty() && !YearRegex.matches(it) }

  val isValid: Boolean
    get() = !isTitleMissing && !isPublishedYearInvalid

  fun isChanged(detail: BookDetail): Boolean = when (detail) {
    BookDetail.Title -> title.isChangedFrom(base.title)
    BookDetail.Subtitle -> subtitle.isChangedFrom(base.subtitle)
    BookDetail.Description -> description.toHtml() != baseDescriptionHtml
    BookDetail.Publisher -> publisher.isChangedFrom(base.publisher)
    BookDetail.PublishedYear -> publishedYear.isChangedFrom(base.publishedYear)
    BookDetail.Language -> language.isChangedFrom(base.language)
    BookDetail.Isbn -> isbn.isChangedFrom(base.ISBN)
    BookDetail.Asin -> asin.isChangedFrom(base.ASIN)
    BookDetail.Explicit -> explicit != base.isExplicit
    BookDetail.Abridged -> abridged != base.isAbridged
  }

  /** What to send the server: only the changed details, a cleared one as empty text. */
  fun update(): BookDetailsUpdate {
    val changes = changes
    fun TextFieldState.ifChanged(detail: BookDetail) = text.toString().trim().takeIf { detail in changes }
    return BookDetailsUpdate(
      title = title.ifChanged(BookDetail.Title),
      subtitle = subtitle.ifChanged(BookDetail.Subtitle),
      description = description.takeIf { BookDetail.Description in changes }?.let {
        if (it.annotatedString.text.isBlank()) "" else it.toHtml()
      },
      publisher = publisher.ifChanged(BookDetail.Publisher),
      publishedYear = publishedYear.ifChanged(BookDetail.PublishedYear),
      language = language.ifChanged(BookDetail.Language),
      isbn = isbn.ifChanged(BookDetail.Isbn),
      asin = asin.ifChanged(BookDetail.Asin),
      explicit = explicit.takeIf { BookDetail.Explicit in changes },
      abridged = abridged.takeIf { BookDetail.Abridged in changes },
    )
  }

  /** What puts back the details [update] changes, as [base] had them. */
  fun undoOf(update: BookDetailsUpdate) = BookDetailsUpdate(
    title = update.title?.let { base.title.orEmpty() },
    subtitle = update.subtitle?.let { base.subtitle.orEmpty() },
    description = update.description?.let { base.description.orEmpty() },
    publisher = update.publisher?.let { base.publisher.orEmpty() },
    publishedYear = update.publishedYear?.let { base.publishedYear.orEmpty() },
    language = update.language?.let { base.language.orEmpty() },
    isbn = update.isbn?.let { base.ISBN.orEmpty() },
    asin = update.asin?.let { base.ASIN.orEmpty() },
    explicit = update.explicit?.let { base.isExplicit },
    abridged = update.abridged?.let { base.isAbridged },
  )

  private fun TextFieldState.isChangedFrom(value: String?): Boolean =
    text.toString().trim() != value.orEmpty().trim()
}

/**
 * Whether the server's copy of the details, [latest], differs from what editing started from in
 * anything the editor changes, as when someone else saved them meanwhile.
 */
fun BookDetailsDraft.isBehind(latest: Media.Metadata): Boolean =
  latest.title != base.title ||
    latest.subtitle != base.subtitle ||
    latest.description != base.description ||
    latest.publisher != base.publisher ||
    latest.publishedYear != base.publishedYear ||
    latest.language != base.language ||
    latest.ISBN != base.ISBN ||
    latest.ASIN != base.ASIN ||
    latest.isExplicit != base.isExplicit ||
    latest.isAbridged != base.isAbridged

private val YearRegex = Regex("""\d{4}""")
