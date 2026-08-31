// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.audible

import app.campfire.bookinfo.api.BookCommunityInfo
import app.campfire.bookinfo.api.BookInfoProvider
import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.BookMatch
import app.campfire.bookinfo.api.BookReview
import app.campfire.bookinfo.api.ProviderCapabilities
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.api.ProviderSeries
import app.campfire.bookinfo.api.SeriesMatch
import app.campfire.bookinfo.audible.di.AudibleClient
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import io.ktor.client.HttpClient
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Inject

/**
 * Keyless audiobook-native source backed by Audible's public catalog API — the
 * same anonymous endpoints the Audiobookshelf server's own Audible metadata
 * provider uses. Keys on ASIN, the identifier audiobooks most reliably carry,
 * and supplies the Audible aggregate rating with its full star distribution.
 *
 * Currently pinned to the US marketplace (api.audible.com); other marketplaces
 * live on per-region hosts and can become a setting later.
 */
@SingleIn(UserScope::class)
@ContributesMultibinding(UserScope::class, boundType = BookInfoProvider::class)
@Inject
class AudibleBookInfoProvider(
  @AudibleClient private val client: HttpClient,
) : BookInfoProvider {

  private val catalog = AudibleCatalog(client)

  override val id: ProviderId = ProviderId.Audible
  override val displayName: String = "Audible"

  override val capabilities: ProviderCapabilities = ProviderCapabilities(
    hasReviewText = true,
    hasAggregateRating = true,
    hasSeriesOrdering = true,
    hasUpcomingReleases = true,
    hasSupplementalMetadata = true,
  )

  override fun observeLinkState(): Flow<ProviderLinkState> =
    flowOf(ProviderLinkState.Linked(accountName = null))

  override fun canServe(match: BookMatch): Boolean =
    match is BookMatch.Identifiers && !match.asin.isNullOrBlank()

  override suspend fun getBookInfo(match: BookMatch): BookInfoResult<BookCommunityInfo> {
    val asin = (match as? BookMatch.Identifiers)?.asin.normalizedAsin()
      ?: return BookInfoResult.NotFound

    val product = when (val result = catalog.product(asin, AudibleCatalog.BOOK_RESPONSE_GROUPS)) {
      is BookInfoResult.Success -> result.data
      is BookInfoResult.Failure -> return result
      else -> return BookInfoResult.NotFound
    }

    val overall = product.rating?.overallDistribution
    val average = overall?.averageRating?.takeIf { it > 0.0 }
      ?: return BookInfoResult.NotFound
    if ((overall.numRatings ?: 0) <= 0) return BookInfoResult.NotFound

    return BookInfoResult.Success(
      BookCommunityInfo(
        providerBookId = product.asin ?: asin,
        providerUrl = audibleProductUrl(product.asin ?: asin),
        rating = average,
        ratingsCount = overall.numRatings,
        ratingsDistribution = overall.toDistribution(),
        reviewsCount = product.rating.numReviews,
        releaseDate = product.releaseDate,
        coverUrl = product.coverUrl(),
      ),
    )
  }

  override suspend fun getReviews(match: BookMatch, limit: Int): BookInfoResult<List<BookReview>> {
    val asin = (match as? BookMatch.Identifiers)?.asin.normalizedAsin()
      ?: return BookInfoResult.NotFound

    return when (val result = catalog.reviews(asin, limit)) {
      is BookInfoResult.Success -> BookInfoResult.Success(
        result.data.mapNotNull { review ->
          // Bodies arrive entity-escaped; one decode pass yields the HTML the
          // review cards already render (real <br/> tags and so on). Audible
          // has no spoiler flag.
          val text = review.body?.decodeHtmlEntities()?.takeUnless { it.isBlank() }
            ?: return@mapNotNull null
          BookReview(
            author = review.authorName?.decodeHtmlEntities()?.takeUnless { it.isBlank() },
            rating = review.ratings?.overallRating?.takeIf { it > 0.0 },
            text = text,
            hasSpoilers = false,
            title = review.title?.decodeHtmlEntities()?.takeUnless { it.isBlank() },
          )
        },
      )
      is BookInfoResult.Failure -> result
      else -> BookInfoResult.Success(emptyList())
    }
  }

  /**
   * Resolves a series through Audible's own edges — no name matching, no
   * candidate guessing: an owned book's ASIN carries its series ASIN, the
   * series parent enumerates every child with its sequence, and one batch
   * lookup hydrates them. Pre-orders appear with future release dates, which
   * is what makes upcoming-book detection possible here.
   */
  override suspend fun getSeries(match: SeriesMatch): BookInfoResult<ProviderSeries> {
    val memberAsins = match.memberMatches
      .mapNotNull { it.asin.normalizedAsin() }
      .distinct()
    if (memberAsins.isEmpty()) return BookInfoResult.NotFound

    // Read the series membership off an owned book; try a few in case the
    // first ASIN is region-foreign or missing from the catalog.
    var membership: AudibleSeriesMembership? = null
    for (asin in memberAsins.take(MAX_MEMBERSHIP_LOOKUPS)) {
      val product = when (
        val result = catalog.product(asin, AudibleCatalog.SERIES_MEMBERSHIP_RESPONSE_GROUPS)
      ) {
        is BookInfoResult.Success -> result.data
        is BookInfoResult.Failure -> return result
        else -> continue
      }
      membership = pickMembership(product.series, match) ?: continue
      break
    }
    val seriesAsin = membership?.asin ?: return BookInfoResult.NotFound

    val parent = when (
      val result = catalog.product(seriesAsin, AudibleCatalog.SERIES_CHILDREN_RESPONSE_GROUPS)
    ) {
      is BookInfoResult.Success -> result.data
      is BookInfoResult.Failure -> return result
      else -> return BookInfoResult.NotFound
    }
    val children = parent.relationships.filter {
      it.relationshipType == "series" && it.relationshipToProduct == "child" && !it.asin.isNullOrBlank()
    }
    if (children.isEmpty()) return BookInfoResult.NotFound

    val products = when (
      val result = catalog.products(
        asins = children.map { it.asin!! },
        responseGroups = AudibleCatalog.BOOK_RESPONSE_GROUPS,
      )
    ) {
      is BookInfoResult.Success -> result.data
      is BookInfoResult.Failure -> return result
      else -> return BookInfoResult.NotFound
    }

    val entries = buildSeriesEntries(
      sequencesByAsin = children.associate { it.asin!! to it.sequence },
      products = products,
      nowIsoDate = todayIsoDate(),
    )
    if (entries.isEmpty()) return BookInfoResult.NotFound

    return BookInfoResult.Success(
      ProviderSeries(
        providerSeriesId = seriesAsin,
        name = membership.title ?: match.seriesName,
        isCompleted = null,
        entries = entries,
      ),
    )
  }

  private fun todayIsoDate(): String {
    return Clock.System.now()
      .toLocalDateTime(TimeZone.currentSystemDefault())
      .date
      .toString()
  }

  companion object {
    private const val MAX_MEMBERSHIP_LOOKUPS = 3
  }
}

internal fun String?.normalizedAsin(): String? {
  return this?.filter { it.isLetterOrDigit() }?.uppercase()?.takeUnless { it.isEmpty() }
}

/**
 * Undoes one level of HTML entity escaping. Audible delivers review text
 * double-encoded: `&lt;br/&gt;` decodes to a real `<br/>` tag for the HTML
 * renderer. `&amp;` is decoded last so it can't create new entities.
 */
internal fun String.decodeHtmlEntities(): String {
  return replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&apos;", "'")
    .replace("&#39;", "'")
    .replace(Regex("&#(\\d+);")) { match ->
      match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
    }
    .replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
      match.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: match.value
    }
    .replace("&amp;", "&")
}

internal fun audibleProductUrl(asin: String): String = "https://www.audible.com/pd/$asin"

internal fun AudibleRatingDistribution.toDistribution(): Map<Int, Int>? {
  val counts = mapOf(
    1 to (numOneStarRatings ?: 0),
    2 to (numTwoStarRatings ?: 0),
    3 to (numThreeStarRatings ?: 0),
    4 to (numFourStarRatings ?: 0),
    5 to (numFiveStarRatings ?: 0),
  )
  return counts.takeIf { it.values.any { count -> count > 0 } }
}
