// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.runtime.Immutable
import app.campfire.core.model.FilterData

/** Names the book's library already has, to suggest while a field is typed in */
@Immutable
data class DetailSuggestions(
  val authors: List<String> = emptyList(),
  val narrators: List<String> = emptyList(),
  val series: List<String> = emptyList(),
  val genres: List<String> = emptyList(),
  val tags: List<String> = emptyList(),
  val publishers: List<String> = emptyList(),
  val languages: List<String> = emptyList(),
)

fun FilterData.asSuggestions() = DetailSuggestions(
  authors = authors.map { it.name },
  narrators = narrators,
  series = series.map { it.name },
  genres = genres,
  tags = tags,
  publishers = publishers,
  languages = languages,
)

/** A name that matches what's typed, at [match] in it */
@Immutable
data class Suggestion(
  val name: String,
  val match: IntRange,
)

/**
 * The [names] that [query] is in, ignoring case, leaving out [excluding]. Names that start with it
 * come first, then names with a word that does, then the rest, at most [limit] of them.
 */
fun suggestionsFor(
  query: String,
  names: List<String>,
  excluding: Collection<String> = emptyList(),
  limit: Int = MaxSuggestions,
): List<Suggestion> {
  val typed = query.trim()
  if (typed.isEmpty()) return emptyList()

  return names.asSequence()
    .mapNotNull { name -> name.rankedMatchOf(typed) }
    .filter { (suggestion, _) -> !excluding.containsIgnoringCase(suggestion.name) }
    // Stable, so names that match alike keep the library's order
    .sortedBy { (_, rank) -> rank }
    .distinctBy { (suggestion, _) -> suggestion.name.lowercase() }
    .take(limit)
    .map { (suggestion, _) -> suggestion }
    .toList()
}

/** This name as a suggestion for [typed], with how well it matches: lower is better */
private fun String.rankedMatchOf(typed: String): Pair<Suggestion, Int>? {
  val first = indexOf(typed, ignoreCase = true)
  if (first < 0) return null

  var start = first
  while (start > 0 && this[start - 1].isLetterOrDigit()) {
    start = indexOf(typed, startIndex = start + 1, ignoreCase = true)
    if (start < 0) break
  }
  val atWord = start >= 0
  val at = if (atWord) start else first
  val rank = when {
    at == 0 -> 0
    atWord -> 1
    else -> 2
  }
  return Suggestion(this, at until at + typed.length) to rank
}

private const val MaxSuggestions = 6
