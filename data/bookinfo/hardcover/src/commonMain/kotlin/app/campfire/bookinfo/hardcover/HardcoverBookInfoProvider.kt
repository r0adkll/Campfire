// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover

import app.campfire.bookinfo.api.AccountLinkable
import app.campfire.bookinfo.api.BookCommunityInfo
import app.campfire.bookinfo.api.BookInfoProvider
import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.BookMatch
import app.campfire.bookinfo.api.BookReview
import app.campfire.bookinfo.api.LinkedAccount
import app.campfire.bookinfo.api.ProviderCapabilities
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.hardcover.auth.HardcoverTokenStorage
import app.campfire.bookinfo.hardcover.graphql.BOOK_REVIEWS_QUERY
import app.campfire.bookinfo.hardcover.graphql.BooksData
import app.campfire.bookinfo.hardcover.graphql.HardcoverBook
import app.campfire.bookinfo.hardcover.graphql.HardcoverGraphQl
import app.campfire.bookinfo.hardcover.graphql.HardcoverGraphQlException
import app.campfire.bookinfo.hardcover.graphql.HardcoverResult
import app.campfire.bookinfo.hardcover.graphql.ME_QUERY
import app.campfire.bookinfo.hardcover.graphql.MeData
import app.campfire.bookinfo.hardcover.graphql.UserBooksData
import app.campfire.bookinfo.hardcover.graphql.bookByIdentifiersQuery
import app.campfire.bookinfo.hardcover.graphql.parseCoverUrl
import app.campfire.bookinfo.hardcover.graphql.parseRatingsDistribution
import app.campfire.bookinfo.hardcover.graphql.parseUsername
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.tatarka.inject.annotations.Inject

class InvalidHardcoverTokenException : Exception("Hardcover rejected the token")

@SingleIn(UserScope::class)
@ContributesMultibinding(UserScope::class, boundType = BookInfoProvider::class)
@Inject
class HardcoverBookInfoProvider(
  private val graphQl: HardcoverGraphQl,
  private val tokenStorage: HardcoverTokenStorage,
  private val userSession: UserSession,
) : BookInfoProvider, AccountLinkable {

  override val id: ProviderId = ProviderId.Hardcover
  override val displayName: String = "Hardcover"
  override val linkHelpUrl: String = "https://hardcover.app/account/api"

  // Series capabilities stay false until the series contract lands in the api.
  override val capabilities: ProviderCapabilities = ProviderCapabilities(
    hasReviewText = true,
    hasAggregateRating = true,
    hasSupplementalMetadata = true,
    requiresAccountLink = true,
  )

  // Match → Hardcover book id, memoized so getReviews doesn't re-resolve a book
  // that getBookInfo already matched this session.
  private val matchedIds = mutableMapOf<BookMatch, Long>()

  override fun observeLinkState(): Flow<ProviderLinkState> {
    val userId = userSession.userId ?: return flowOf(ProviderLinkState.NotLinked)
    return tokenStorage.observeLinkState(userId)
  }

  override suspend fun getBookInfo(match: BookMatch): BookInfoResult<BookCommunityInfo> {
    val (document, variables) = when (match) {
      is BookMatch.Identifiers -> {
        val isbn = match.isbn?.filter { it.isLetterOrDigit() }?.takeUnless { it.isEmpty() }
        val asin = match.asin?.trim()?.takeUnless { it.isEmpty() }
        if (isbn == null && asin == null) return BookInfoResult.NotFound
        bookByIdentifiersQuery(hasIsbn = isbn != null, hasAsin = asin != null) to buildJsonObject {
          isbn?.let { put("isbn", it) }
          asin?.let { put("asin", it) }
        }
      }
      // Hardcover disables _like/_regex predicates; title matching needs their
      // `search` endpoint, deferred until its response shape is verified live.
      is BookMatch.TitleAuthor -> return BookInfoResult.NotFound
    }

    return when (val result = graphQl.execute(document, variables, BooksData.serializer())) {
      is HardcoverResult.Success -> {
        val book = result.data.books.firstOrNull() ?: return BookInfoResult.NotFound
        matchedIds[match] = book.id
        BookInfoResult.Success(book.toCommunityInfo())
      }
      else -> result.toBookInfoError()
    }
  }

  override suspend fun getReviews(match: BookMatch, limit: Int): BookInfoResult<List<BookReview>> {
    val bookId = matchedIds[match] ?: when (val resolved = getBookInfo(match)) {
      is BookInfoResult.Success -> matchedIds[match] ?: return BookInfoResult.NotFound
      is BookInfoResult.NotFound -> return resolved
      is BookInfoResult.NotLinked -> return resolved
      is BookInfoResult.TokenInvalid -> return resolved
      is BookInfoResult.RateLimited -> return resolved
      is BookInfoResult.Failure -> return resolved
    }

    val variables = buildJsonObject {
      put("bookId", bookId)
      put("limit", limit)
    }

    return when (val result = graphQl.execute(BOOK_REVIEWS_QUERY, variables, UserBooksData.serializer())) {
      is HardcoverResult.Success -> BookInfoResult.Success(
        result.data.userBooks.mapNotNull { userBook ->
          val text = userBook.review?.takeUnless { it.isBlank() } ?: return@mapNotNull null
          BookReview(
            author = userBook.user?.name?.takeUnless { it.isBlank() } ?: userBook.user?.username,
            rating = userBook.rating,
            text = text,
            hasSpoilers = userBook.hasSpoilers ?: false,
            avatarUrl = parseCoverUrl(userBook.user?.cachedImage),
            badge = userBook.user?.flair?.takeUnless { it.isBlank() },
          )
        },
      )
      else -> result.toBookInfoError()
    }
  }

  override suspend fun verifyAndLink(token: String): Result<LinkedAccount> {
    val userId = userSession.userId
      ?: return Result.failure(IllegalStateException("No active user session"))
    val trimmed = token.trim()
    if (trimmed.isEmpty()) return Result.failure(InvalidHardcoverTokenException())

    return when (val result = graphQl.executeWithToken(trimmed, ME_QUERY, deserializer = MeData.serializer())) {
      is HardcoverResult.Success -> {
        val username = parseUsername(result.data.me)
        tokenStorage.link(userId, trimmed, username)
        Result.success(LinkedAccount(username))
      }
      is HardcoverResult.TokenInvalid -> Result.failure(InvalidHardcoverTokenException())
      is HardcoverResult.RateLimited -> Result.failure(Exception("Hardcover rate limit reached, try again shortly"))
      is HardcoverResult.GraphQlErrors -> Result.failure(HardcoverGraphQlException(result.messages))
      is HardcoverResult.NetworkFailure -> Result.failure(result.cause)
      is HardcoverResult.NotLinked -> Result.failure(InvalidHardcoverTokenException())
    }
  }

  override suspend fun unlink() {
    val userId = userSession.userId ?: return
    matchedIds.clear()
    tokenStorage.unlink(userId)
  }

  private fun HardcoverBook.toCommunityInfo(): BookCommunityInfo = BookCommunityInfo(
    providerBookId = id.toString(),
    providerUrl = slug?.let { "https://hardcover.app/books/$it" },
    rating = rating,
    ratingsCount = ratingsCount,
    ratingsDistribution = parseRatingsDistribution(ratingsDistribution),
    reviewsCount = reviewsCount,
    releaseDate = releaseDate,
    coverUrl = parseCoverUrl(cachedImage),
  )

  private fun HardcoverResult<*>.toBookInfoError(): BookInfoResult<Nothing> = when (this) {
    is HardcoverResult.Success -> error("Not an error result")
    is HardcoverResult.NotLinked -> BookInfoResult.NotLinked
    is HardcoverResult.TokenInvalid -> BookInfoResult.TokenInvalid
    is HardcoverResult.RateLimited -> BookInfoResult.RateLimited(retryAfter)
    is HardcoverResult.GraphQlErrors -> BookInfoResult.Failure(HardcoverGraphQlException(messages))
    is HardcoverResult.NetworkFailure -> BookInfoResult.Failure(cause)
  }
}
