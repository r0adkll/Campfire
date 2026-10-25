// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.campfire.common.compose.extensions.toRichTextHtml
import app.campfire.core.model.Media
import app.campfire.core.model.SeriesSequence
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.SeriesEntry
import com.mohamedrejeb.richeditor.model.RichTextState

/** A detail of a book that the editor changes. */
enum class BookDetail {
  Cover,
  Title,
  Subtitle,
  Authors,
  Narrators,
  Series,
  Description,
  Publisher,
  PublishedYear,
  Language,
  Genres,
  Tags,
  Isbn,
  Asin,
  Explicit,
  Abridged,
}

/** Why a series row can't be saved */
enum class SeriesRowError {
  /** It has a number but no series to go with it */
  MissingName,

  /** An earlier row already names the series */
  Duplicate,
}

/** A series the book is in, as it's being edited: its name and the book's number in it */
@Stable
class SeriesRowState(name: String = "", sequence: String = "") {
  val name = TextFieldState(name)
  val sequence = TextFieldState(sequence)
}

/**
 * A book's details as they're being edited, from [base] and [baseTags], the details as the server
 * had them when editing started. Each field is edited in place; what differs from the base is what
 * a save sends.
 */
@Stable
class BookDetailsDraft(
  val base: Media.Metadata,
  val baseTags: List<String> = emptyList(),
) {

  val title = TextFieldState(base.title.orEmpty())
  val subtitle = TextFieldState(base.subtitle.orEmpty())
  val publisher = TextFieldState(base.publisher.orEmpty())
  val publishedYear = TextFieldState(base.publishedYear.orEmpty())
  val language = TextFieldState(base.language.orEmpty())
  val isbn = TextFieldState(base.ISBN.orEmpty())
  val asin = TextFieldState(base.ASIN.orEmpty())
  var explicit by mutableStateOf(base.isExplicit)
  var abridged by mutableStateOf(base.isAbridged)

  val authors = TokenListState(base.authorNames)
  val narrators = TokenListState(base.narrators)
  val genres = TokenListState(base.genres)
  val tags = TokenListState(baseTags)

  val series = mutableStateListOf<SeriesRowState>().apply {
    base.series.forEach { add(SeriesRowState(it.name, it.formattedSequence)) }
  }

  /** A cover for the server to download in place of the book's own, as a match offers */
  var newCoverUrl by mutableStateOf<String?>(null)

  /** Edited with its formatting, which the server keeps as HTML */
  val description = RichTextState().apply {
    setHtml(base.description.orEmpty().toRichTextHtml(linkify = false))
  }

  // What the editor makes of the description as it came, which an untouched one still matches
  private val baseDescriptionHtml = descriptionHtml()

  /** The details that differ from [base]. */
  val changes: Set<BookDetail>
    get() = BookDetail.entries.filterTo(mutableSetOf()) { isChanged(it) }

  val isTitleMissing: Boolean
    get() = title.text.toString().isBlank()

  /** A year the server would take: four digits, or nothing */
  val isPublishedYearInvalid: Boolean
    get() = publishedYear.text.toString().trim().let { it.isNotEmpty() && !YearRegex.matches(it) }

  val isValid: Boolean
    get() = !isTitleMissing && !isPublishedYearInvalid && series.indices.none { seriesErrorAt(it) != null }

  /** What keeps the series row at [index] from being saved, if anything */
  fun seriesErrorAt(index: Int): SeriesRowError? {
    val row = series[index]
    val name = row.name.text.toString().trim()
    return when {
      name.isEmpty() -> SeriesRowError.MissingName.takeIf { row.sequence.text.isNotBlank() }
      series.take(index).any { it.name.text.toString().trim().equals(name, ignoreCase = true) } ->
        SeriesRowError.Duplicate
      else -> null
    }
  }

  fun addSeries() {
    series += SeriesRowState()
  }

  fun removeSeries(row: SeriesRowState) {
    series.remove(row)
  }

  fun isChanged(detail: BookDetail): Boolean = when (detail) {
    BookDetail.Cover -> newCoverUrl != null
    BookDetail.Title -> title.isChangedFrom(base.title)
    BookDetail.Subtitle -> subtitle.isChangedFrom(base.subtitle)
    // The server matches authors by name ignoring case, in an order of its own
    BookDetail.Authors -> authors.values.lowercaseSet() != base.authorNames.lowercaseSet()
    BookDetail.Narrators -> narrators.values != base.narrators
    BookDetail.Series -> seriesEntries().byName() != base.series.asEntries().byName()
    BookDetail.Description -> descriptionHtml() != baseDescriptionHtml
    BookDetail.Publisher -> publisher.isChangedFrom(base.publisher)
    BookDetail.PublishedYear -> publishedYear.isChangedFrom(base.publishedYear)
    BookDetail.Language -> language.isChangedFrom(base.language)
    BookDetail.Genres -> genres.values != base.genres
    BookDetail.Tags -> tags.values != baseTags
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
      description = descriptionHtml().takeIf { BookDetail.Description in changes },
      publisher = publisher.ifChanged(BookDetail.Publisher),
      publishedYear = publishedYear.ifChanged(BookDetail.PublishedYear),
      language = language.ifChanged(BookDetail.Language),
      isbn = isbn.ifChanged(BookDetail.Isbn),
      asin = asin.ifChanged(BookDetail.Asin),
      explicit = explicit.takeIf { BookDetail.Explicit in changes },
      abridged = abridged.takeIf { BookDetail.Abridged in changes },
      authors = authors.values.takeIf { BookDetail.Authors in changes },
      narrators = narrators.values.takeIf { BookDetail.Narrators in changes },
      series = seriesEntries().takeIf { BookDetail.Series in changes },
      genres = genres.values.takeIf { BookDetail.Genres in changes },
      tags = tags.values.takeIf { BookDetail.Tags in changes },
      coverUrl = newCoverUrl,
    )
  }

  /**
   * What puts back the details [update] changes, as [base] had them. A new cover takes the place
   * of the old one on the server, so it can't be put back.
   */
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
    authors = update.authors?.let { base.authorNames },
    narrators = update.narrators?.let { base.narrators },
    series = update.series?.let { base.series.asEntries() },
    genres = update.genres?.let { base.genres },
    tags = update.tags?.let { baseTags },
  )

  /** The series rows as the server takes them: named ones, trimmed, a missing number as none */
  private fun seriesEntries(): List<SeriesEntry> = series.mapNotNull { row ->
    val name = row.name.text.toString().trim()
    if (name.isEmpty()) return@mapNotNull null
    SeriesEntry(name, row.sequence.text.toString().trim().ifEmpty { null })
  }

  /**
   * The description as HTML, or empty text once it's cleared. Typing changes the editor's
   * paragraphs in place, out of sight of whatever reads this in a snapshot; its text is state, so
   * reading that first has them read this again.
   */
  private fun descriptionHtml(): String =
    if (description.annotatedString.text.isBlank()) "" else description.toHtml()

  private fun TextFieldState.isChangedFrom(value: String?): Boolean =
    text.toString().trim() != value.orEmpty().trim()
}

/**
 * Whether the server's copy of the details, [latest] and [latestTags], differs from what editing
 * started from in anything the editor changes, as when someone else saved them meanwhile.
 */
fun BookDetailsDraft.isBehind(latest: Media.Metadata, latestTags: List<String>): Boolean =
  latest.title != base.title ||
    latest.subtitle != base.subtitle ||
    latest.description != base.description ||
    latest.publisher != base.publisher ||
    latest.publishedYear != base.publishedYear ||
    latest.language != base.language ||
    latest.ISBN != base.ISBN ||
    latest.ASIN != base.ASIN ||
    latest.isExplicit != base.isExplicit ||
    latest.isAbridged != base.isAbridged ||
    latest.authorNames != base.authorNames ||
    latest.narrators != base.narrators ||
    latest.series.asEntries() != base.series.asEntries() ||
    latest.genres != base.genres ||
    latestTags != baseTags

private val Media.Metadata.authorNames: List<String>
  get() = authors.map { it.name }

/** The series as the editor shows them, which is what an untouched row sends back */
private fun List<SeriesSequence>.asEntries(): List<SeriesEntry> = map {
  SeriesEntry(it.name.trim(), it.formattedSequence.trim().ifEmpty { null })
}

/** Series by name, ignoring case as the server does, with each one's number */
private fun List<SeriesEntry>.byName(): Map<String, String?> = associate { it.name.lowercase() to it.sequence }

private fun List<String>.lowercaseSet(): Set<String> = mapTo(mutableSetOf()) { it.lowercase() }

private val YearRegex = Regex("""\d{4}""")
