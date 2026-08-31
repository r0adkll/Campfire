// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.test

import app.campfire.bookinfo.api.BookCommunityInfo
import app.campfire.bookinfo.api.BookInfoProvider
import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.BookMatch
import app.campfire.bookinfo.api.BookReview
import app.campfire.bookinfo.api.ProviderCapabilities
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBookInfoProvider(
  override val id: ProviderId = ProviderId.Hardcover,
  override val displayName: String = "Fake Provider",
  override var capabilities: ProviderCapabilities = ProviderCapabilities(
    hasReviewText = true,
    hasAggregateRating = true,
    hasSeriesOrdering = true,
    hasUpcomingReleases = true,
    hasSupplementalMetadata = true,
    requiresAccountLink = true,
  ),
) : BookInfoProvider {

  val linkState = MutableStateFlow<ProviderLinkState>(ProviderLinkState.Linked(accountName = null))
  override fun observeLinkState(): Flow<ProviderLinkState> = linkState

  var canServeResult: Boolean = true
  override fun canServe(match: BookMatch): Boolean = canServeResult

  var bookInfoResult: BookInfoResult<BookCommunityInfo> = BookInfoResult.NotFound
  val bookInfoRequests = mutableListOf<BookMatch>()
  override suspend fun getBookInfo(match: BookMatch): BookInfoResult<BookCommunityInfo> {
    bookInfoRequests += match
    return bookInfoResult
  }

  var reviewsResult: BookInfoResult<List<BookReview>> = BookInfoResult.Success(emptyList())
  val reviewsRequests = mutableListOf<BookMatch>()
  override suspend fun getReviews(match: BookMatch, limit: Int): BookInfoResult<List<BookReview>> {
    reviewsRequests += match
    return reviewsResult
  }
}
