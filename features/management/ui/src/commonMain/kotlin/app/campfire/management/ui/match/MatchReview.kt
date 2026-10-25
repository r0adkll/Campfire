// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Immutable
import app.campfire.common.compose.extensions.toRichTextHtml
import app.campfire.libraries.api.SeriesEntry
import app.campfire.management.api.match.BookMatch
import app.campfire.management.ui.editor.BookDetailsDraft
import app.campfire.management.ui.editor.SeriesRowState
import app.campfire.management.ui.editor.StagedCover

/** A detail a match can fill in, in the order a review lists them */
enum class MatchField {
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

/** How a match's list joins the list the book has */
enum class ListMerge {
  Replace,
  Add,
}

/** Which of a review's differing details to take at once */
enum class SelectionMode {
  All,

  /** Only the details the book doesn't have yet */
  EmptyOnly,
  None,
}

/** A detail's value, as a review shows it */
@Immutable
sealed interface MatchValue {
  data class Text(val text: String) : MatchValue
  data class Names(val names: List<String>) : MatchValue
  data class Series(val entries: List<SeriesEntry>) : MatchValue
  data class Flag(val isOn: Boolean) : MatchValue
  data class Image(val url: String) : MatchValue
}

/** One detail of a match beside what the book has now, null when it has nothing */
@Immutable
data class MatchRow(
  val field: MatchField,
  val new: MatchValue,
  val current: MatchValue?,
) {
  /** The book already has exactly this, as far as the server would tell */
  val isSame: Boolean
    get() = when {
      current == null -> false
      // An image's address says nothing about whether it's the same picture
      new is MatchValue.Image -> false
      // In an accessor, a bare `field` is the property's own backing field
      this.field == MatchField.Authors || this.field == MatchField.Series -> new.lowercase() == current.lowercase()
      this.field == MatchField.Description -> new.plainText() == current.plainText()
      else -> new == current
    }

  val isListMergeable: Boolean
    get() = this.field == MatchField.Genres || this.field == MatchField.Tags
}

/**
 * The details of [match] beside what [draft] has now, for each one the match has. [coverUrl] is
 * the book's own cover, if it has one.
 */
fun reviewRows(
  match: BookMatch,
  draft: BookDetailsDraft,
  coverUrl: String?,
): List<MatchRow> = buildList {
  fun text(field: MatchField, new: String?, current: String) {
    if (new != null) {
      add(
        MatchRow(
          field,
          MatchValue.Text(new),
          current.trim().takeIf {
            it.isNotEmpty()
          }?.let(MatchValue::Text),
        ),
      )
    }
  }
  fun names(field: MatchField, new: List<String>, current: List<String>) {
    if (new.isNotEmpty()) {
      add(
        MatchRow(
          field,
          MatchValue.Names(new),
          current.takeIf {
            it.isNotEmpty()
          }?.let(MatchValue::Names),
        ),
      )
    }
  }
  fun flag(field: MatchField, new: Boolean?, current: Boolean) {
    if (new != null) add(MatchRow(field, MatchValue.Flag(new), MatchValue.Flag(current)))
  }

  match.coverUrl?.let { add(MatchRow(MatchField.Cover, MatchValue.Image(it), coverUrl?.let(MatchValue::Image))) }
  text(MatchField.Title, match.title, draft.title.text.toString())
  text(MatchField.Subtitle, match.subtitle, draft.subtitle.text.toString())
  names(MatchField.Authors, match.authors, draft.authors.values)
  names(MatchField.Narrators, match.narrators, draft.narrators.values)
  if (match.series.isNotEmpty()) {
    val current = draft.series.mapNotNull { row ->
      val name = row.name.text.toString().trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
      SeriesEntry(name, row.sequence.text.toString().trim().takeIf { it.isNotEmpty() })
    }
    add(
      MatchRow(
        MatchField.Series,
        MatchValue.Series(match.series),
        current.takeIf {
          it.isNotEmpty()
        }?.let(MatchValue::Series),
      ),
    )
  }
  text(MatchField.Description, match.description, draft.description.annotatedString.text)
  text(MatchField.Publisher, match.publisher, draft.publisher.text.toString())
  text(MatchField.PublishedYear, match.publishedYear, draft.publishedYear.text.toString())
  text(MatchField.Language, match.language, draft.language.text.toString())
  names(MatchField.Genres, match.genres, draft.genres.values)
  names(MatchField.Tags, match.tags, draft.tags.values)
  text(MatchField.Isbn, match.isbn, draft.isbn.text.toString())
  text(MatchField.Asin, match.asin, draft.asin.text.toString())
  flag(MatchField.Explicit, match.explicit, draft.explicit)
  flag(MatchField.Abridged, match.abridged, draft.abridged)
}

/** The rows [mode] takes: those that would change something, and with [SelectionMode.EmptyOnly], fill a gap */
fun List<MatchRow>.selectedBy(mode: SelectionMode, canChangeCover: Boolean): Set<MatchField> =
  filter { row ->
    !row.isSame &&
      (row.field != MatchField.Cover || canChangeCover) &&
      when (mode) {
        SelectionMode.All -> true
        SelectionMode.EmptyOnly -> row.current == null
        SelectionMode.None -> false
      }
  }.mapTo(mutableSetOf()) { it.field }

/**
 * Fills [draft] with the [fields] of [match], the lists among them joined as [merges] says, so
 * saving sends them like any other edit.
 */
fun BookDetailsDraft.fillFrom(
  match: BookMatch,
  fields: Set<MatchField>,
  merges: Map<MatchField, ListMerge> = emptyMap(),
) {
  fun ListMerge?.isAdding() = this == ListMerge.Add
  for (field in fields) {
    when (field) {
      MatchField.Cover -> newCover = match.coverUrl?.let(StagedCover::fromUrl)
      MatchField.Title -> match.title?.let { title.setTextAndPlaceCursorAtEnd(it) }
      MatchField.Subtitle -> match.subtitle?.let { subtitle.setTextAndPlaceCursorAtEnd(it) }
      MatchField.Authors -> authors.replaceWith(match.authors)
      MatchField.Narrators -> narrators.replaceWith(match.narrators)
      MatchField.Series -> {
        series.clear()
        match.series.forEach { series += SeriesRowState(it.name, it.sequence.orEmpty()) }
      }
      MatchField.Description -> match.description?.let { description.setHtml(it.toRichTextHtml(linkify = false)) }
      MatchField.Publisher -> match.publisher?.let { publisher.setTextAndPlaceCursorAtEnd(it) }
      MatchField.PublishedYear -> match.publishedYear?.let { publishedYear.setTextAndPlaceCursorAtEnd(it) }
      MatchField.Language -> match.language?.let { language.setTextAndPlaceCursorAtEnd(it) }
      MatchField.Genres -> if (merges[field].isAdding()) {
        genres.addAll(match.genres)
      } else genres.replaceWith(
        match.genres,
      )
      MatchField.Tags -> if (merges[field].isAdding()) tags.addAll(match.tags) else tags.replaceWith(match.tags)
      MatchField.Isbn -> match.isbn?.let { isbn.setTextAndPlaceCursorAtEnd(it) }
      MatchField.Asin -> match.asin?.let { asin.setTextAndPlaceCursorAtEnd(it) }
      MatchField.Explicit -> match.explicit?.let { explicit = it }
      MatchField.Abridged -> match.abridged?.let { abridged = it }
    }
  }
}

private fun MatchValue.plainText(): String? = (this as? MatchValue.Text)?.text?.plain()

private fun MatchValue.lowercase(): MatchValue = when (this) {
  is MatchValue.Names -> copy(names = names.map { it.lowercase() }.sorted())
  is MatchValue.Series -> copy(entries = entries.map { it.copy(name = it.name.lowercase()) }.sortedBy { it.name })
  else -> this
}

/** A description as text alone, without its markup and spacing, to compare */
internal fun String.plain(): String = replace(BreakRegex, " ")
  .replace(TagRegex, "")
  .replace("&nbsp;", " ")
  .replace("&amp;", "&")
  .replace("&lt;", "<")
  .replace("&gt;", ">")
  .replace("&quot;", "\"")
  .replace("&#39;", "'")
  .replace(SpaceRegex, " ")
  .trim()

private val BreakRegex = Regex("""<br\s*/?>|</p>""", RegexOption.IGNORE_CASE)
private val TagRegex = Regex("""<[^>]+>""")
private val SpaceRegex = Regex("""\s+""")
