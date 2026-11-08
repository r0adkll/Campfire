// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import app.campfire.core.model.LibraryItemId
import app.campfire.management.api.match.BookMatch
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.api.match.MetadataMatchRepository
import app.campfire.management.api.match.QuickMatchResult

class FakeMetadataMatchRepository : MetadataMatchRepository {

  var providersResult: Result<List<MatchProvider>> = Result.success(listOf(Audible, Google))
  var providerLoads = 0

  data class Search(
    val itemId: LibraryItemId,
    val provider: String,
    val title: String,
    val author: String?,
  )

  val searches = mutableListOf<Search>()
  var searchResult: (Search) -> Result<List<BookMatch>> = { Result.success(emptyList()) }

  override var lastProvider: String? = null

  override suspend fun getBookProviders(): Result<List<MatchProvider>> {
    providerLoads++
    return providersResult
  }

  override suspend fun searchBooks(
    itemId: LibraryItemId,
    provider: String,
    title: String,
    author: String?,
  ): Result<List<BookMatch>> {
    val search = Search(itemId, provider, title, author)
    searches += search
    lastProvider = provider
    return searchResult(search)
  }

  override suspend fun quickMatchBook(
    itemId: LibraryItemId,
    provider: String,
    replaceDetails: Boolean,
    replaceCover: Boolean,
  ): Result<QuickMatchResult> = Result.failure(NotImplementedError("The editor doesn't quick match"))

  companion object {
    val Audible = MatchProvider("audible", "Audible.com")
    val Google = MatchProvider("google", "Google Books")
  }
}
