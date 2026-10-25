// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management

import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.SeriesSequence
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.SeriesEntry
import app.campfire.management.api.match.BookMatch
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.api.match.MetadataMatchRepository
import app.campfire.management.api.match.QuickMatchResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.envelopes.QuickMatchRequest
import app.campfire.network.isForbidden
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
  private val libraryItemRepository: LibraryItemRepository,
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

  override suspend fun quickMatchBook(
    itemId: LibraryItemId,
    provider: String,
    replaceDetails: Boolean,
    replaceCover: Boolean,
  ): Result<QuickMatchResult> {
    // The server's own copy, to tell what the match changes. A cached one can be a list's, with
    // no authors, narrators or series to put back.
    val before = libraryItemRepository.fetchLibraryItem(itemId).getOrElse { return Result.failure(it) }
    val request = QuickMatchRequest(provider, overrideDetails = replaceDetails, overrideCover = replaceCover)
    return api.quickMatchLibraryItem(itemId, request)
      .explainRefusal()
      .map { response ->
        when {
          // Fetching it again caches the change too, which sockets would but can be turned off
          response.updated -> changesBetween(before, libraryItemRepository.fetchLibraryItem(itemId).getOrNull())
          response.warning != null -> QuickMatchResult.NoMatch
          else -> QuickMatchResult.NothingChanged
        }
      }
  }

  private fun changesBetween(before: LibraryItem, after: LibraryItem?): QuickMatchResult.Updated {
    val old = before.media as? Media.Book
    val new = after?.media as? Media.Book
    if (old == null || new == null) return QuickMatchResult.Updated(undo = null, coverChanged = false)

    val undo = old.undoOf(new)
    return QuickMatchResult.Updated(
      undo = undo,
      // The server reported a change, and a cover is the only one that leaves the details alone
      coverChanged = (old.coverPath == null && new.coverPath != null) || undo.isEmpty,
    )
  }

  private fun <T> Result<T>.explainRefusal(): Result<T> {
    val error = exceptionOrNull()
    return if (error?.isForbidden == true) Result.failure(ItemChangeNotAllowedException(error)) else this
  }
}

/**
 * What puts back the details [after] has changed from this book's, as this book has them: one
 * field for each changed detail, compared as the editor does.
 */
internal fun Media.Book.undoOf(after: Media.Book): BookDetailsUpdate {
  val old = metadata
  val new = after.metadata
  fun text(field: (Media.Metadata) -> String?): String? =
    field(old).orEmpty().takeIf { it.trim() != field(new).orEmpty().trim() }

  return BookDetailsUpdate(
    title = text { it.title },
    subtitle = text { it.subtitle },
    description = text { it.description },
    publisher = text { it.publisher },
    publishedYear = text { it.publishedYear },
    language = text { it.language },
    isbn = text { it.ISBN },
    asin = text { it.ASIN },
    explicit = old.isExplicit.takeIf { it != new.isExplicit },
    abridged = old.isAbridged.takeIf { it != new.isAbridged },
    // The server matches authors and series by name ignoring case, in an order of its own
    authors = old.authorNames.takeIf { it.lowercaseSet() != new.authorNames.lowercaseSet() },
    narrators = old.narrators.takeIf { it != new.narrators },
    series = old.series.asEntries().takeIf { it.byName() != new.series.asEntries().byName() },
    genres = old.genres.takeIf { it != new.genres },
    tags = tags.takeIf { it != after.tags },
  )
}

private val Media.Metadata.authorNames: List<String>
  get() = authors.map { it.name }

private fun List<SeriesSequence>.asEntries(): List<SeriesEntry> = map {
  SeriesEntry(it.name.trim(), it.formattedSequence.trim().ifEmpty { null })
}

private fun List<SeriesEntry>.byName(): Map<String, String?> = associate { it.name.lowercase() to it.sequence }

private fun List<String>.lowercaseSet(): Set<String> = mapTo(mutableSetOf()) { it.lowercase() }

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
