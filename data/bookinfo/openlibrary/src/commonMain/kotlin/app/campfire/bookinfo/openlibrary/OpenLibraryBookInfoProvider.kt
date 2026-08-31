// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.openlibrary

import app.campfire.bookinfo.api.BookCommunityInfo
import app.campfire.bookinfo.api.BookInfoProvider
import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.BookMatch
import app.campfire.bookinfo.api.BookReview
import app.campfire.bookinfo.api.ProviderCapabilities
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.openlibrary.di.OpenLibraryClient
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

/**
 * Keyless aggregate-rating and metadata source backed by Open Library
 * (openlibrary.org). No account, no token — always available as a fallback
 * when no account-linked provider is connected. Open Library keys on ISBN, so
 * ASIN-only audiobooks are declared unservable and left to other providers.
 */
@SingleIn(UserScope::class)
@ContributesMultibinding(UserScope::class, boundType = BookInfoProvider::class)
@Inject
class OpenLibraryBookInfoProvider(
  @OpenLibraryClient private val client: HttpClient,
) : BookInfoProvider {

  override val id: ProviderId = ProviderId.OpenLibrary
  override val displayName: String = "Open Library"

  override val capabilities: ProviderCapabilities = ProviderCapabilities(
    hasAggregateRating = true,
    hasSupplementalMetadata = true,
  )

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  private val throttle = RequestThrottle()

  override fun observeLinkState(): Flow<ProviderLinkState> =
    flowOf(ProviderLinkState.Linked(accountName = null))

  override fun canServe(match: BookMatch): Boolean =
    match is BookMatch.Identifiers && !match.isbn.isNullOrBlank()

  override suspend fun getBookInfo(match: BookMatch): BookInfoResult<BookCommunityInfo> {
    val isbn = (match as? BookMatch.Identifiers)
      ?.isbn
      ?.filter { it.isLetterOrDigit() }
      ?.takeUnless { it.isEmpty() }
      ?: return BookInfoResult.NotFound

    // Two hops: the edition resolves the ISBN to a work, and ratings hang off
    // the work. Covers are referenced by cover id, which dodges Open Library's
    // tight per-IP limit on ISBN-keyed cover lookups.
    val edition = when (val result = fetch("$BASE_URL/isbn/$isbn.json", OpenLibraryEdition.serializer())) {
      is Fetched.Success -> result.value
      Fetched.NotFound -> return BookInfoResult.NotFound
      is Fetched.Failure -> return BookInfoResult.Failure(result.cause)
    }
    val workKey = edition.works.firstOrNull()?.key
      ?.takeIf { it.startsWith("/works/") }
      ?: return BookInfoResult.NotFound

    val ratings = when (val result = fetch("$BASE_URL$workKey/ratings.json", OpenLibraryRatings.serializer())) {
      is Fetched.Success -> result.value
      Fetched.NotFound -> return BookInfoResult.NotFound
      is Fetched.Failure -> return BookInfoResult.Failure(result.cause)
    }

    // No ratings is treated as a miss: the aggregate rating is what this
    // provider exists to supply, and misses retry on the short TTL.
    val average = ratings.summary.average ?: return BookInfoResult.NotFound
    if ((ratings.summary.count ?: 0) <= 0) return BookInfoResult.NotFound

    return BookInfoResult.Success(
      BookCommunityInfo(
        providerBookId = workKey,
        providerUrl = "$BASE_URL$workKey",
        rating = average,
        ratingsCount = ratings.summary.count,
        ratingsDistribution = ratings.counts
          .mapNotNull { (star, count) -> star.toIntOrNull()?.let { it to count } }
          .toMap()
          .ifEmpty { null },
        reviewsCount = null,
        releaseDate = edition.publishDate,
        coverUrl = edition.covers.firstOrNull { it > 0 }
          ?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" },
      ),
    )
  }

  override suspend fun getReviews(match: BookMatch, limit: Int): BookInfoResult<List<BookReview>> {
    return BookInfoResult.Success(emptyList())
  }

  private sealed interface Fetched<out T> {
    data class Success<T>(val value: T) : Fetched<T>
    data object NotFound : Fetched<Nothing>
    data class Failure(val cause: Throwable) : Fetched<Nothing>
  }

  private suspend fun <T> fetch(
    url: String,
    deserializer: kotlinx.serialization.DeserializationStrategy<T>,
  ): Fetched<T> {
    val response: HttpResponse = try {
      throttle.withThrottle { client.get(url) }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      return Fetched.Failure(e)
    }

    return when {
      response.status == HttpStatusCode.NotFound -> Fetched.NotFound
      !response.status.isSuccess() -> Fetched.Failure(OpenLibraryHttpException(response.status.value))
      else -> try {
        Fetched.Success(json.decodeFromString(deserializer, response.bodyAsText()))
      } catch (e: Exception) {
        Fetched.Failure(e)
      }
    }
  }

  companion object {
    private const val BASE_URL = "https://openlibrary.org"
  }
}

class OpenLibraryHttpException(val status: Int) : Exception("Open Library HTTP $status")

@Serializable
internal data class OpenLibraryEdition(
  val works: List<OpenLibraryWorkRef> = emptyList(),
  val covers: List<Long> = emptyList(),
  @SerialName("publish_date") val publishDate: String? = null,
)

@Serializable
internal data class OpenLibraryWorkRef(
  val key: String? = null,
)

@Serializable
internal data class OpenLibraryRatings(
  val summary: OpenLibraryRatingsSummary = OpenLibraryRatingsSummary(),
  val counts: Map<String, Int> = emptyMap(),
)

@Serializable
internal data class OpenLibraryRatingsSummary(
  val average: Double? = null,
  val count: Int? = null,
)
