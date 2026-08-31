// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.audnexus

import app.campfire.bookinfo.api.BookCommunityInfo
import app.campfire.bookinfo.api.BookInfoProvider
import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.BookMatch
import app.campfire.bookinfo.api.BookReview
import app.campfire.bookinfo.api.ProviderCapabilities
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.audnexus.di.AudnexusClient
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import io.ktor.client.HttpClient
import io.ktor.client.request.get
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
 * Keyless audiobook-native source backed by Audnexus (audnex.us), whose data
 * is Audible-derived. Keys on ASIN — the identifier audiobooks most reliably
 * carry — and supplies the Audible aggregate rating plus metadata. No rating
 * distribution or count is available.
 */
@SingleIn(UserScope::class)
@ContributesMultibinding(UserScope::class, boundType = BookInfoProvider::class)
@Inject
class AudnexusBookInfoProvider(
  @AudnexusClient private val client: HttpClient,
) : BookInfoProvider {

  override val id: ProviderId = ProviderId.Audnexus
  override val displayName: String = "Audnexus"

  override val capabilities: ProviderCapabilities = ProviderCapabilities(
    hasAggregateRating = true,
    hasSupplementalMetadata = true,
  )

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  override fun observeLinkState(): Flow<ProviderLinkState> =
    flowOf(ProviderLinkState.Linked(accountName = null))

  override fun canServe(match: BookMatch): Boolean =
    match is BookMatch.Identifiers && !match.asin.isNullOrBlank()

  override suspend fun getBookInfo(match: BookMatch): BookInfoResult<BookCommunityInfo> {
    val asin = (match as? BookMatch.Identifiers)
      ?.asin
      ?.filter { it.isLetterOrDigit() }
      ?.uppercase()
      ?.takeUnless { it.isEmpty() }
      ?: return BookInfoResult.NotFound

    val response = try {
      client.get("$BASE_URL/books/$asin")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      return BookInfoResult.Failure(e)
    }

    return when {
      response.status == HttpStatusCode.NotFound -> BookInfoResult.NotFound
      // Audnexus reports an unparseable/unknown ASIN as a 400.
      response.status == HttpStatusCode.BadRequest -> BookInfoResult.NotFound
      !response.status.isSuccess() ->
        BookInfoResult.Failure(AudnexusHttpException(response.status.value))
      else -> try {
        val book = json.decodeFromString(AudnexusBook.serializer(), response.bodyAsText())
        val rating = book.rating?.toDoubleOrNull()?.takeIf { it > 0.0 }
          ?: return BookInfoResult.NotFound
        BookInfoResult.Success(
          BookCommunityInfo(
            providerBookId = book.asin ?: asin,
            providerUrl = "https://www.audible.com/pd/${book.asin ?: asin}",
            rating = rating,
            ratingsCount = null,
            ratingsDistribution = null,
            reviewsCount = null,
            releaseDate = book.releaseDate,
            coverUrl = book.image,
          ),
        )
      } catch (e: Exception) {
        BookInfoResult.Failure(e)
      }
    }
  }

  override suspend fun getReviews(match: BookMatch, limit: Int): BookInfoResult<List<BookReview>> {
    return BookInfoResult.Success(emptyList())
  }

  companion object {
    private const val BASE_URL = "https://api.audnex.us"
  }
}

class AudnexusHttpException(val status: Int) : Exception("Audnexus HTTP $status")

@Serializable
internal data class AudnexusBook(
  val asin: String? = null,
  val title: String? = null,
  val rating: String? = null,
  @SerialName("releaseDate") val releaseDate: String? = null,
  val image: String? = null,
)
