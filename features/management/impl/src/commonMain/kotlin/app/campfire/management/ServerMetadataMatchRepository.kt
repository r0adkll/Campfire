// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management

import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.libraries.api.SeriesEntry
import app.campfire.management.api.match.BookMatch
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.api.match.MetadataMatchRepository
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.isNotFound
import app.campfire.network.models.BookMatchResult
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class ServerMetadataMatchRepository(
  private val api: AudioBookShelfApi,
) : MetadataMatchRepository {

  override var lastProvider: String? = null

  override suspend fun getBookProviders(): Result<List<MatchProvider>> {
    val listed = api.getSearchProviders()
      .map { response -> response.providers.books.map { MatchProvider(it.value, it.text) } }
    return when {
      listed.getOrNull()?.isNotEmpty() == true -> listed
      // Before 2.31 the server has no list, but always has these
      listed.isSuccess || listed.exceptionOrNull()?.isNotFound == true -> Result.success(BuiltInProviders)
      else -> listed
    }
  }

  override suspend fun searchBooks(
    itemId: LibraryItemId,
    provider: String,
    title: String,
    author: String?,
  ): Result<List<BookMatch>> {
    lastProvider = provider
    return api.searchBooks(
      provider = provider,
      title = title.trim(),
      author = author?.trim()?.takeIf { it.isNotEmpty() },
      libraryItemId = itemId,
    ).map { results -> results.map { it.asBookMatch() } }
  }
}

private fun BookMatchResult.asBookMatch() = BookMatch(
  title = title.orNullIfBlank(),
  subtitle = subtitle.orNullIfBlank(),
  authors = author.asNames(),
  narrators = narrator.asNames(),
  publisher = publisher.orNullIfBlank(),
  publishedYear = publishedYear.orNullIfBlank(),
  description = description.orNullIfBlank(),
  coverUrl = cover.orNullIfBlank(),
  isbn = isbn.orNullIfBlank(),
  asin = asin.orNullIfBlank(),
  genres = genres.asNames(),
  tags = tags.asNames(),
  series = series.orEmpty().mapNotNull { series ->
    val name = series.series.orNullIfBlank() ?: return@mapNotNull null
    SeriesEntry(name, series.sequence.orNullIfBlank())
  },
  language = language.orNullIfBlank(),
  // Audible gives 0 when it doesn't know
  duration = duration?.takeIf { it > 0 }?.minutes,
  confidence = matchConfidence?.toFloat(),
  explicit = explicit,
  abridged = abridged,
)

/** Names that come as a list, or as one text joined with commas */
private fun JsonElement?.asNames(): List<String> {
  val names = when (this) {
    is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    is JsonPrimitive -> contentOrNull?.split(',').orEmpty()
    else -> emptyList()
  }
  return names.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}

private fun String?.orNullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** What servers before 2.31, which don't list them, can search */
private val BuiltInProviders = listOf(
  MatchProvider("audible", "Audible.com"),
  MatchProvider("audible.ca", "Audible.ca"),
  MatchProvider("audible.uk", "Audible.co.uk"),
  MatchProvider("audible.au", "Audible.com.au"),
  MatchProvider("audible.fr", "Audible.fr"),
  MatchProvider("audible.de", "Audible.de"),
  MatchProvider("audible.jp", "Audible.co.jp"),
  MatchProvider("audible.it", "Audible.it"),
  MatchProvider("audible.in", "Audible.in"),
  MatchProvider("audible.es", "Audible.es"),
  MatchProvider("google", "Google Books"),
  MatchProvider("itunes", "iTunes"),
  MatchProvider("openlibrary", "Open Library"),
  MatchProvider("fantlab", "FantLab.ru"),
)
