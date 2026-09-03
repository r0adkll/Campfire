// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats

import app.campfire.account.test.FakeUrlHydrator
import app.campfire.core.time.FatherTime
import app.campfire.network.models.LibraryStats
import app.campfire.network.models.ListeningStats
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.test.FakeUserRepository
import app.campfire.user.test.fixtures.user
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NetworkStatsRepositoryTest {

  private val api = FakeAudioBookShelfApi()
  private val userRepository = FakeUserRepository()
  private val fatherTime = FakeFatherTime()

  private val repository = NetworkStatsRepository(
    api = api,
    userRepository = userRepository,
    urlHydrator = FakeUrlHydrator(),
    fatherTime = fatherTime,
  )

  private var userStatsFetches = 0
  private var libraryStatsFetches = 0

  init {
    userRepository.currentUserFlow.tryEmit(user("fake_user_id"))
    api.listeningStatsResult = {
      userStatsFetches++
      Result.success(networkListeningStats(totalTime = userStatsFetches.toFloat()))
    }
    api.libraryStatsResult = {
      libraryStatsFetches++
      Result.success(networkLibraryStats(totalItems = libraryStatsFetches))
    }
  }

  @Test
  fun `user stats are served from the cache within the ttl`() = runTest {
    assertThat(repository.getUserStats().first().totalTime).isEqualTo(1.seconds)

    fatherTime.nowMillis += NetworkStatsRepository.UserStatsTtl.inWholeMilliseconds - 1

    assertThat(repository.getUserStats().first().totalTime).isEqualTo(1.seconds)
    assertThat(userStatsFetches).isEqualTo(1)
  }

  @Test
  fun `user stats are refetched after the ttl expires`() = runTest {
    repository.getUserStats().first()

    fatherTime.nowMillis += NetworkStatsRepository.UserStatsTtl.inWholeMilliseconds + 1

    assertThat(repository.getUserStats().first().totalTime).isEqualTo(2.seconds)
    assertThat(userStatsFetches).isEqualTo(2)
  }

  @Test
  fun `stale user stats are served when the refetch fails`() = runTest {
    repository.getUserStats().first()

    fatherTime.nowMillis += NetworkStatsRepository.UserStatsTtl.inWholeMilliseconds + 1
    api.listeningStatsResult = { Result.failure(RuntimeException("server unreachable")) }

    assertThat(repository.getUserStats().first().totalTime).isEqualTo(1.seconds)
  }

  @Test
  fun `user stats fail when there is no cache and the fetch fails`() = runTest {
    api.listeningStatsResult = { Result.failure(RuntimeException("server unreachable")) }

    assertThat(
      runCatching { repository.getUserStats().first() },
    ).isFailure()
  }

  @Test
  fun `refresh forces a refetch within the ttl and updates active collectors`() = runTest {
    repository.getUserStats().test {
      assertThat(awaitItem().totalTime).isEqualTo(1.seconds)

      val result = repository.refresh()

      assertThat(result.isSuccess).isEqualTo(true)
      assertThat(awaitItem().totalTime).isEqualTo(2.seconds)
      assertThat(userStatsFetches).isEqualTo(2)
      assertThat(libraryStatsFetches).isEqualTo(1)
    }
  }

  @Test
  fun `refresh returns failure and keeps the cache when the fetch fails`() = runTest {
    repository.getUserStats().first()
    repository.getLibraryStats().first()

    api.listeningStatsResult = { Result.failure(RuntimeException("server unreachable")) }

    val result = repository.refresh()

    assertThat(result.isFailure).isEqualTo(true)
    assertThat(repository.getUserStats().first().totalTime).isEqualTo(1.seconds)
  }

  @Test
  fun `library stats are served from the cache within the ttl`() = runTest {
    assertThat(repository.getLibraryStats().first().totalItems).isEqualTo(1)

    fatherTime.nowMillis += NetworkStatsRepository.LibraryStatsTtl.inWholeMilliseconds - 1

    assertThat(repository.getLibraryStats().first().totalItems).isEqualTo(1)
    assertThat(libraryStatsFetches).isEqualTo(1)
  }

  @Test
  fun `library stats are refetched after the ttl expires`() = runTest {
    repository.getLibraryStats().first()

    fatherTime.nowMillis += NetworkStatsRepository.LibraryStatsTtl.inWholeMilliseconds + 1

    assertThat(repository.getLibraryStats().first().totalItems).isEqualTo(2)
    assertThat(libraryStatsFetches).isEqualTo(2)
  }
}

private class FakeFatherTime(
  var nowMillis: Long = 0L,
) : FatherTime {
  override fun now(): LocalDateTime =
    Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(TimeZone.UTC)

  override fun today(): LocalDate = now().date

  override fun nowInEpochMillis(): Long = nowMillis
}

private fun networkListeningStats(
  totalTime: Float = 0f,
) = ListeningStats(
  totalTime = totalTime,
  today = 0f,
  days = emptyMap(),
  dayOfWeek = emptyMap(),
  items = emptyMap(),
  recentSessions = emptyList(),
)

private fun networkLibraryStats(
  totalItems: Int = 0,
) = LibraryStats(
  largestItems = emptyList(),
  totalAuthors = null,
  authorsWithCount = null,
  totalGenres = 0,
  genresWithCount = emptyList(),
  totalItems = totalItems,
  longestItems = emptyList(),
  totalSize = 0L,
  totalDuration = 0.0,
  numAudioTracks = 0,
)
