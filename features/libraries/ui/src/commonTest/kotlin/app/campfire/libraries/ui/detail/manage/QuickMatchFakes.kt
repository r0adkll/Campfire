// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaType
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.paging.LibraryItemPager
import app.campfire.management.api.match.BookMatch
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.api.match.MetadataMatchRepository
import app.campfire.management.api.match.QuickMatchResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/** The libraries the app has cached, which is all a quick match asks of them. */
class FakeLibraryRepository(
  libraries: List<Library> = listOf(library("fakeLibraryId", provider = "audible")),
) : LibraryRepository {

  val libraries = MutableStateFlow(libraries)

  override fun observeCurrentLibrary(refresh: Boolean): Flow<Library> = emptyFlow()
  override fun observeAllLibraries(refresh: Boolean): Flow<List<Library>> = libraries
  override fun observeCurrentLibraryItems(refresh: Boolean): Flow<List<LibraryItem>> = flowOf(emptyList())
  override fun createLibraryItemPager(
    user: User,
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): LibraryItemPager = throw NotImplementedError("Quick matching doesn't page")

  override fun observeFilteredLibraryCount(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Int?> = flowOf(null)

  override suspend fun setCurrentLibrary(library: Library) = Unit

  override suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext> =
    Result.failure(NotImplementedError("Quick matching doesn't add podcasts"))
}

/** The server's sources and its quick match, which [quickMatchResult] answers. */
class FakeQuickMatchRepository : MetadataMatchRepository {

  var providersResult: Result<List<MatchProvider>> = Result.success(
    listOf(MatchProvider("audible", "Audible.com"), MatchProvider("google", "Google Books")),
  )

  data class QuickMatch(
    val itemId: LibraryItemId,
    val provider: String,
    val replaceDetails: Boolean,
    val replaceCover: Boolean,
  )

  val quickMatches = mutableListOf<QuickMatch>()
  var quickMatchResult: Result<QuickMatchResult> = Result.success(QuickMatchResult.NothingChanged)

  /** When set, a quick match waits for it, as a slow search would */
  var quickMatchGate: CompletableDeferred<Unit>? = null

  override var lastProvider: String? = null

  override suspend fun getBookProviders(): Result<List<MatchProvider>> = providersResult

  override suspend fun searchBooks(
    itemId: LibraryItemId,
    provider: String,
    title: String,
    author: String?,
  ): Result<List<BookMatch>> = Result.failure(NotImplementedError("A quick match doesn't search"))

  override suspend fun quickMatchBook(
    itemId: LibraryItemId,
    provider: String,
    replaceDetails: Boolean,
    replaceCover: Boolean,
  ): Result<QuickMatchResult> {
    quickMatches += QuickMatch(itemId, provider, replaceDetails, replaceCover)
    quickMatchGate?.await()
    return quickMatchResult
  }
}

internal fun quickMatcher(
  libraryRepository: LibraryRepository = FakeLibraryRepository(),
  matchRepository: MetadataMatchRepository = FakeQuickMatchRepository(),
  libraryItemRepository: LibraryItemRepository,
  scope: CoroutineScope,
) = QuickMatcher(
  libraryRepository = libraryRepository,
  matchRepository = matchRepository,
  libraryItemRepository = libraryItemRepository,
  userScope = CoroutineScopeHolder { scope },
)

fun library(id: LibraryId, provider: String) = Library(
  id = id,
  name = "Audiobooks",
  displayOrder = 1,
  icon = Library.Icon.Books1,
  mediaType = MediaType.Book,
  provider = provider,
  coverAspectRatio = 1,
  audiobooksOnly = false,
  createdAt = 0L,
  lastUpdate = 0L,
)
