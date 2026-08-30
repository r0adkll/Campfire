// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.store

import app.campfire.bookinfo.api.BookInfoProvider
import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.BookMatch
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.db.BookInfoDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
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

class BookInfoNotLinkedException : Exception("Provider is not linked")
class BookInfoTokenInvalidException : Exception("Provider token was rejected")
class BookInfoRateLimitedException : Exception("Provider rate limit reached")

/**
 * Store5 pipeline for [CachedBookInfo]: fetches from the keyed provider and
 * persists in the bookinfo cache database. Callers decide refresh policy via
 * [stream]'s `refresh` flag (see [isStale]); everything read serves from the
 * source of truth first, which is what keeps Campfire inside provider rate
 * limits.
 */
@SingleIn(UserScope::class)
@Inject
class BookInfoStore(
  private val providers: Set<BookInfoProvider>,
  private val db: BookInfoDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  data class Key(
    val userId: UserId,
    val providerId: ProviderId,
    val libraryItemId: LibraryItemId,
    val match: BookMatch,
  )

  private val json = Json { ignoreUnknownKeys = true }

  private val store: Store<Key, CachedBookInfo> = StoreBuilder.from(
    fetcher = Fetcher.ofResult { key: Key -> fetch(key) },
    sourceOfTruth = SourceOfTruth.of<Key, CachedBookInfo, CachedBookInfo>(
      reader = { key ->
        db.bookInfoCacheQueries
          .select(key.userId, key.providerId.key, key.libraryItemId)
          .asFlow()
          .mapToOneOrNull(dispatcherProvider.io)
          .map { row -> row?.let { decode(it.payload) } }
      },
      writer = { key, value ->
        db.bookInfoCacheQueries.upsert(
          userId = key.userId,
          providerId = key.providerId.key,
          libraryItemId = key.libraryItemId,
          payload = json.encodeToString(CachedBookInfo.serializer(), value),
          fetchedAt = value.fetchedAt,
        )
      },
      delete = { key ->
        db.bookInfoCacheQueries.delete(key.userId, key.providerId.key, key.libraryItemId)
      },
      deleteAll = {
        db.bookInfoCacheQueries.deleteAll()
      },
    ),
  ).build()

  fun stream(key: Key, refresh: Boolean): Flow<StoreReadResponse<CachedBookInfo>> {
    return store.stream(StoreReadRequest.cached(key, refresh = refresh))
  }

  suspend fun clearAll() {
    store.clear()
  }

  suspend fun cached(key: Key): CachedBookInfo? {
    return db.bookInfoCacheQueries
      .select(key.userId, key.providerId.key, key.libraryItemId)
      .awaitAsOneOrNull()
      ?.let { decode(it.payload) }
  }

  private suspend fun fetch(key: Key): FetcherResult<CachedBookInfo> {
    val provider = providers.firstOrNull { it.id == key.providerId }
      ?: return FetcherResult.Error.Message("No provider installed for ${key.providerId.key}")

    return when (val result = provider.getBookInfo(key.match)) {
      is BookInfoResult.Success -> {
        val reviews = if (provider.capabilities.hasReviewText) {
          (provider.getReviews(key.match) as? BookInfoResult.Success)?.data.orEmpty()
        } else {
          emptyList()
        }
        FetcherResult.Data(
          CachedBookInfo(
            info = result.data,
            reviews = reviews,
            fetchedAt = nowMillis(),
            matchKey = key.match.cacheKey,
          ),
        )
      }

      // Cache the miss so unknown books aren't re-queried on every screen open
      // (short TTL — see CachedBookInfo).
      is BookInfoResult.NotFound -> FetcherResult.Data(
        CachedBookInfo(
          info = null,
          reviews = emptyList(),
          fetchedAt = nowMillis(),
          matchKey = key.match.cacheKey,
        ),
      )

      is BookInfoResult.NotLinked -> FetcherResult.Error.Exception(BookInfoNotLinkedException())
      is BookInfoResult.TokenInvalid -> FetcherResult.Error.Exception(BookInfoTokenInvalidException())
      is BookInfoResult.RateLimited -> FetcherResult.Error.Exception(BookInfoRateLimitedException())
      is BookInfoResult.Failure -> FetcherResult.Error.Exception(result.cause)
    }
  }

  private fun decode(payload: String): CachedBookInfo? {
    return try {
      json.decodeFromString(CachedBookInfo.serializer(), payload)
    } catch (e: Exception) {
      null
    }
  }

  private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
