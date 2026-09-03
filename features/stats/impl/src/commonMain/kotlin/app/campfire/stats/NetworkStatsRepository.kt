// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats

import app.campfire.account.api.UrlHydrator
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryStats
import app.campfire.core.model.ListeningStats
import app.campfire.core.time.FatherTime
import app.campfire.data.mapping.asDomainModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.stats.api.StatsRepository
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class NetworkStatsRepository(
  private val api: AudioBookShelfApi,
  private val userRepository: UserRepository,
  private val urlHydrator: UrlHydrator,
  private val fatherTime: FatherTime,
) : StatsRepository {

  private val userStatsCache = MutableStateFlow<Cached<ListeningStats>?>(null)
  private val userStatsMutex = Mutex()

  private val libraryStatsCache = MutableStateFlow<Map<LibraryId, Cached<LibraryStats>>>(emptyMap())
  private val libraryStatsMutex = Mutex()

  override fun getUserStats(): Flow<ListeningStats> {
    return flow {
      if (userStatsCache.value.isExpired(UserStatsTtl)) {
        try {
          fetchUserStats(force = false)
        } catch (e: Exception) {
          // Serve the stale cache when the server is unreachable; only fail
          // when there is nothing to show at all
          if (userStatsCache.value == null) throw e
        }
      }
      emitAll(
        userStatsCache
          .filterNotNull()
          .map { it.value },
      )
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun getLibraryStats(): Flow<LibraryStats> {
    return userRepository.observeCurrentUser()
      .map { it.selectedLibraryId }
      .distinctUntilChanged()
      .flatMapLatest { libraryId ->
        flow {
          if (libraryStatsCache.value[libraryId].isExpired(LibraryStatsTtl)) {
            try {
              fetchLibraryStats(libraryId, force = false)
            } catch (e: Exception) {
              if (libraryStatsCache.value[libraryId] == null) throw e
            }
          }
          emitAll(
            libraryStatsCache
              .mapNotNull { it[libraryId]?.value }
              .distinctUntilChanged(),
          )
        }
      }
  }

  override suspend fun refresh(): Result<Unit> = runCatching {
    coroutineScope {
      val userStats = async { fetchUserStats(force = true) }
      val libraryStats = async {
        val libraryId = userRepository.observeCurrentUser().first().selectedLibraryId
        fetchLibraryStats(libraryId, force = true)
      }
      userStats.await()
      libraryStats.await()
    }
  }

  private suspend fun fetchUserStats(force: Boolean) {
    userStatsMutex.withLock {
      // Freshness is re-checked under the lock so concurrent collectors coalesce
      // into a single fetch instead of racing duplicate requests
      if (!force && !userStatsCache.value.isExpired(UserStatsTtl)) return

      val stats = api.getListeningStats().getOrThrow().asDomainModel(urlHydrator)
      userStatsCache.value = Cached(stats, fatherTime.nowInEpochMillis())
    }
  }

  private suspend fun fetchLibraryStats(libraryId: LibraryId, force: Boolean) {
    libraryStatsMutex.withLock {
      if (!force && !libraryStatsCache.value[libraryId].isExpired(LibraryStatsTtl)) return

      val stats = api.getLibraryStats(libraryId).getOrThrow().asDomainModel(urlHydrator)
      libraryStatsCache.value = libraryStatsCache.value +
        (libraryId to Cached(stats, fatherTime.nowInEpochMillis()))
    }
  }

  private fun Cached<*>?.isExpired(ttl: Duration): Boolean {
    if (this == null) return true
    return fatherTime.nowInEpochMillis() - fetchedAtMillis > ttl.inWholeMilliseconds
  }

  private data class Cached<T>(
    val value: T,
    val fetchedAtMillis: Long,
  )

  companion object {
    val UserStatsTtl = 5.minutes
    val LibraryStatsTtl = 1.hours
  }
}
