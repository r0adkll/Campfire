// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.store

import app.campfire.bookinfo.api.BookInfoProvider
import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.SeriesMatch
import app.campfire.bookinfo.db.BookInfoDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.UserId
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Store5 pipeline for a provider's canonical series listing, mirroring
 * [BookInfoStore]. Series listings change rarely, so these rows carry a long
 * TTL — the identifier-based [CachedSeriesInfo.matchKey] handles the case that
 * actually matters (the user adding a book to the series).
 */
@SingleIn(UserScope::class)
@Inject
class SeriesInfoStore(
  private val providers: Set<BookInfoProvider>,
  private val db: BookInfoDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  data class Key(
    val userId: UserId,
    val providerId: ProviderId,
    val match: SeriesMatch,
  )

  private val json = Json { ignoreUnknownKeys = true }

  private val store: Store<Key, CachedSeriesInfo> = StoreBuilder.from(
    fetcher = Fetcher.ofResult { key: Key -> fetch(key) },
    sourceOfTruth = SourceOfTruth.of<Key, CachedSeriesInfo, CachedSeriesInfo>(
      reader = { key ->
        db.seriesInfoCacheQueries
          .select(key.userId, key.providerId.key, key.match.seriesName)
          .asFlow()
          .mapToOneOrNull(dispatcherProvider.io)
          .map { row -> row?.let { decode(it.payload) } }
      },
      writer = { key, value ->
        db.seriesInfoCacheQueries.upsert(
          userId = key.userId,
          providerId = key.providerId.key,
          seriesName = key.match.seriesName,
          payload = json.encodeToString(CachedSeriesInfo.serializer(), value),
          fetchedAt = value.fetchedAt,
        )
      },
      delete = { key ->
        db.seriesInfoCacheQueries.delete(key.userId, key.providerId.key, key.match.seriesName)
      },
      deleteAll = {
        db.seriesInfoCacheQueries.deleteAll()
      },
    ),
  ).build()

  fun stream(key: Key, refresh: Boolean): Flow<StoreReadResponse<CachedSeriesInfo>> {
    return store.stream(StoreReadRequest.cached(key, refresh = refresh))
  }

  suspend fun clearAll() {
    store.clear()
  }

  suspend fun cached(key: Key): CachedSeriesInfo? {
    return db.seriesInfoCacheQueries
      .select(key.userId, key.providerId.key, key.match.seriesName)
      .awaitAsOneOrNull()
      ?.let { decode(it.payload) }
  }

  private suspend fun fetch(key: Key): FetcherResult<CachedSeriesInfo> {
    val provider = providers.firstOrNull { it.id == key.providerId }
      ?: return FetcherResult.Error.Message("No provider installed for ${key.providerId.key}")

    return when (val result = provider.getSeries(key.match)) {
      is BookInfoResult.Success -> FetcherResult.Data(
        CachedSeriesInfo(
          series = result.data,
          fetchedAt = nowMillis(),
          matchKey = key.match.cacheKey,
        ),
      )

      // Cache the miss so unmatched series aren't re-queried on every visit.
      is BookInfoResult.NotFound -> FetcherResult.Data(
        CachedSeriesInfo(series = null, fetchedAt = nowMillis(), matchKey = key.match.cacheKey),
      )

      is BookInfoResult.NotLinked -> FetcherResult.Error.Exception(BookInfoNotLinkedException())
      is BookInfoResult.TokenInvalid -> FetcherResult.Error.Exception(BookInfoTokenInvalidException())
      is BookInfoResult.RateLimited -> FetcherResult.Error.Exception(BookInfoRateLimitedException())
      is BookInfoResult.Failure -> FetcherResult.Error.Exception(result.cause)
    }
  }

  private fun decode(payload: String): CachedSeriesInfo? {
    return try {
      json.decodeFromString(CachedSeriesInfo.serializer(), payload)
    } catch (e: Exception) {
      null
    }
  }

  private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
