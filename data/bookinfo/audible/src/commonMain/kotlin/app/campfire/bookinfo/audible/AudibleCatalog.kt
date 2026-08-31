// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.audible

import app.campfire.bookinfo.api.BookInfoResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Thin wrapper over Audible's anonymous catalog endpoints. Single products come
 * back as `{product: {...}}`, batch lookups as `{products: [...]}`.
 */
internal class AudibleCatalog(
  private val client: HttpClient,
) {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  suspend fun product(
    asin: String,
    responseGroups: String,
  ): BookInfoResult<AudibleProduct> {
    return fetch(
      url = "$BASE_URL/catalog/products/$asin?response_groups=$responseGroups&image_sizes=$IMAGE_SIZE",
      deserializer = AudibleProductEnvelope.serializer(),
    ).mapNotNull { it.product }
  }

  suspend fun products(
    asins: List<String>,
    responseGroups: String,
  ): BookInfoResult<List<AudibleProduct>> {
    if (asins.isEmpty()) return BookInfoResult.Success(emptyList())
    val all = mutableListOf<AudibleProduct>()
    // The batch endpoint caps around 50 ASINs per request.
    asins.chunked(BATCH_SIZE).forEach { chunk ->
      val url = "$BASE_URL/catalog/products" +
        "?asins=${chunk.joinToString(",")}" +
        "&response_groups=$responseGroups&image_sizes=$IMAGE_SIZE"
      when (val result = fetch(url, AudibleProductsEnvelope.serializer())) {
        is BookInfoResult.Success -> all += result.data.products
        is BookInfoResult.Failure -> return result
        else -> Unit
      }
    }
    return BookInfoResult.Success(all)
  }

  suspend fun reviews(
    asin: String,
    limit: Int,
  ): BookInfoResult<List<AudibleReview>> {
    return fetch(
      url = "$BASE_URL/catalog/products/$asin/reviews?num_results=$limit&sort_by=MostHelpful",
      deserializer = AudibleReviewsEnvelope.serializer(),
    ).mapNotNull { it.customerReviews }
  }

  private suspend fun <T> fetch(
    url: String,
    deserializer: DeserializationStrategy<T>,
  ): BookInfoResult<T> {
    val response = try {
      client.get(url)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      return BookInfoResult.Failure(e)
    }

    return when {
      response.status == HttpStatusCode.NotFound -> BookInfoResult.NotFound
      // Audible reports an unparseable/unknown ASIN as a 400.
      response.status == HttpStatusCode.BadRequest -> BookInfoResult.NotFound
      !response.status.isSuccess() ->
        BookInfoResult.Failure(AudibleHttpException(response.status.value))
      else -> try {
        BookInfoResult.Success(json.decodeFromString(deserializer, response.bodyAsText()))
      } catch (e: Exception) {
        BookInfoResult.Failure(e)
      }
    }
  }

  companion object {
    private const val BASE_URL = "https://api.audible.com/1.0"
    private const val IMAGE_SIZE = 500
    private const val BATCH_SIZE = 50

    const val BOOK_RESPONSE_GROUPS = "product_attrs,rating,media"
    const val SERIES_MEMBERSHIP_RESPONSE_GROUPS = "series"
    const val SERIES_CHILDREN_RESPONSE_GROUPS = "relationships,product_attrs"
  }
}

internal inline fun <T, R> BookInfoResult<T>.mapNotNull(
  transform: (T) -> R?,
): BookInfoResult<R> = when (this) {
  is BookInfoResult.Success -> transform(data)?.let { BookInfoResult.Success(it) }
    ?: BookInfoResult.NotFound
  is BookInfoResult.NotFound -> this
  is BookInfoResult.NotLinked -> this
  is BookInfoResult.TokenInvalid -> this
  is BookInfoResult.RateLimited -> this
  is BookInfoResult.Failure -> this
}

class AudibleHttpException(val status: Int) : Exception("Audible HTTP $status")

@Serializable
internal data class AudibleProductEnvelope(
  val product: AudibleProduct? = null,
)

@Serializable
internal data class AudibleProductsEnvelope(
  val products: List<AudibleProduct> = emptyList(),
)

@Serializable
internal data class AudibleProduct(
  val asin: String? = null,
  val title: String? = null,
  @SerialName("release_date") val releaseDate: String? = null,
  @SerialName("content_delivery_type") val contentDeliveryType: String? = null,
  val rating: AudibleRating? = null,
  @SerialName("product_images") val productImages: Map<String, String> = emptyMap(),
  val series: List<AudibleSeriesMembership> = emptyList(),
  val relationships: List<AudibleRelationship> = emptyList(),
) {
  fun coverUrl(): String? = productImages.values.firstOrNull()
}

@Serializable
internal data class AudibleRating(
  @SerialName("num_reviews") val numReviews: Int? = null,
  @SerialName("overall_distribution") val overallDistribution: AudibleRatingDistribution? = null,
)

@Serializable
internal data class AudibleRatingDistribution(
  @SerialName("average_rating") val averageRating: Double? = null,
  @SerialName("num_ratings") val numRatings: Int? = null,
  @SerialName("num_one_star_ratings") val numOneStarRatings: Int? = null,
  @SerialName("num_two_star_ratings") val numTwoStarRatings: Int? = null,
  @SerialName("num_three_star_ratings") val numThreeStarRatings: Int? = null,
  @SerialName("num_four_star_ratings") val numFourStarRatings: Int? = null,
  @SerialName("num_five_star_ratings") val numFiveStarRatings: Int? = null,
)

@Serializable
internal data class AudibleReviewsEnvelope(
  @SerialName("customer_reviews") val customerReviews: List<AudibleReview> = emptyList(),
)

@Serializable
internal data class AudibleReview(
  val title: String? = null,
  @SerialName("author_name") val authorName: String? = null,
  val body: String? = null,
  val ratings: AudibleReviewRatings? = null,
)

@Serializable
internal data class AudibleReviewRatings(
  @SerialName("overall_rating") val overallRating: Double? = null,
)

@Serializable
internal data class AudibleSeriesMembership(
  val asin: String? = null,
  val title: String? = null,
  val sequence: String? = null,
)

@Serializable
internal data class AudibleRelationship(
  val asin: String? = null,
  @SerialName("relationship_type") val relationshipType: String? = null,
  @SerialName("relationship_to_product") val relationshipToProduct: String? = null,
  val sequence: String? = null,
)
