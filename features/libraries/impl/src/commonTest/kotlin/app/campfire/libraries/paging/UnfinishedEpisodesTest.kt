// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.dao.SqlDelightLibraryItemDao
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.libraries.StoreLibraryRepository
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.test.FakeUserRepository
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalPagingApi::class)
class UnfinishedEpisodesTest {

  private val api = FakeAudioBookShelfApi()
  private val fatherTime = FixedFatherTime(LOADED_AT)
  private lateinit var db: CampfireDatabase
  private lateinit var repository: StoreLibraryRepository

  @Test
  fun `each podcast counts the episodes the server has unfinished`() = countsTest {
    load(podcast("pod_1", numEpisodesIncomplete = 3), podcast("pod_2", numEpisodesIncomplete = 0), book("book_1"))

    assertThat(counts()).isEqualTo(mapOf("pod_1" to 3, "pod_2" to 0))
  }

  @Test
  fun `an episode finished after the page loaded comes off the count`() = countsTest {
    load(podcast("pod_1", numEpisodesIncomplete = 3))

    db.episodeProgress("pod_1", "ep_1", isFinished = true, finishedAt = LOADED_AT + 1)

    assertThat(counts()).isEqualTo(mapOf("pod_1" to 2))
  }

  @Test
  fun `an episode finished before the page loaded isn't taken off again`() = countsTest {
    db.episodeProgress("pod_1", "ep_1", isFinished = true, finishedAt = LOADED_AT - 1)
    load(podcast("pod_1", numEpisodesIncomplete = 3))

    // Finished on another device before the page loaded, but only reaching this one now
    db.episodeProgress("pod_1", "ep_2", isFinished = true, finishedAt = LOADED_AT - 1)

    assertThat(counts()).isEqualTo(mapOf("pod_1" to 3))
  }

  @Test
  fun `an episode marked unfinished after the page loaded goes back on the count`() = countsTest {
    db.episodeProgress("pod_1", "ep_1", isFinished = true, finishedAt = LOADED_AT - 1)
    load(podcast("pod_1", numEpisodesIncomplete = 3))

    db.episodeProgress("pod_1", "ep_1", isFinished = false, finishedAt = null)

    assertThat(counts()).isEqualTo(mapOf("pod_1" to 4))
  }

  @Test
  fun `an episode finished again after it was marked unfinished counts as it loaded`() = countsTest {
    db.episodeProgress("pod_1", "ep_1", isFinished = true, finishedAt = LOADED_AT - 1)
    load(podcast("pod_1", numEpisodesIncomplete = 3))

    db.episodeProgress("pod_1", "ep_1", isFinished = false, finishedAt = null)
    db.episodeProgress("pod_1", "ep_1", isFinished = true, finishedAt = LOADED_AT + 1)

    assertThat(counts()).isEqualTo(mapOf("pod_1" to 3))
  }

  @Test
  fun `the count stops at zero`() = countsTest {
    load(podcast("pod_1", numEpisodesIncomplete = 1))

    db.episodeProgress("pod_1", "ep_1", isFinished = true, finishedAt = LOADED_AT + 1)
    db.episodeProgress("pod_1", "ep_2", isFinished = true, finishedAt = LOADED_AT + 1)

    assertThat(counts()).isEqualTo(mapOf("pod_1" to 0))
  }

  @Test
  fun `a refresh takes the server's new count`() = countsTest {
    load(podcast("pod_1", numEpisodesIncomplete = 3))
    db.episodeProgress("pod_1", "ep_1", isFinished = true, finishedAt = LOADED_AT + 1)

    // The server has since heard about ep_1 and downloaded two new episodes
    fatherTime.nowMillis = LOADED_AT + 10
    load(podcast("pod_1", numEpisodesIncomplete = 4))

    assertThat(counts()).isEqualTo(mapOf("pod_1" to 4))
  }

  @Test
  fun `the counts follow progress as it changes`() = countsTest {
    load(podcast("pod_1", numEpisodesIncomplete = 3))

    observe().test {
      assertThat(awaitItem()).isEqualTo(mapOf("pod_1" to 3))

      // Playing an episode writes progress without changing the count
      db.episodeProgress("pod_1", "ep_1", isFinished = false, finishedAt = null)
      expectNoEvents()

      db.episodeProgress("pod_1", "ep_1", isFinished = true, finishedAt = LOADED_AT + 1)
      assertThat(awaitItem()).isEqualTo(mapOf("pod_1" to 2))
    }
  }

  private suspend fun load(vararg items: LibraryItemMinified) {
    api.libraryItemsMinifiedResult = { _, _ -> Result.success(page(*items)) }
    mediator.load(LoadType.REFRESH, emptyState())
  }

  private fun observe() = repository.observeUnfinishedEpisodes(
    filter = TestInput.filter,
    sortMode = TestInput.sortMode,
    sortDirection = TestInput.sortDirection,
  )

  private suspend fun counts() = observe().first()

  private lateinit var mediator: LibraryItemRemoteMediator

  private fun countsTest(block: suspend TestScope.() -> Unit) = runTest {
    val driver = createDriver()
    try {
      db = DatabaseFactory(driver).build()
      val dispatcherProvider = asTestDispatcherProvider()
      val dao = SqlDelightLibraryItemDao(
        db = db,
        urlHydrator = FakeUrlHydrator(),
        dispatcherProvider = dispatcherProvider,
        userSession = UserSession.LoggedIn(TestUser),
      )
      mediator = LibraryItemRemoteMediator(
        user = TestUser,
        input = TestInput,
        api = api,
        db = db,
        dispatcherProvider = dispatcherProvider,
        fatherTime = fatherTime,
        urlHydrator = FakeUrlHydrator(),
      )
      val userRepository = FakeUserRepository().apply { currentUserFlow.tryEmit(TestUser) }
      repository = StoreLibraryRepository(
        userSession = UserSession.LoggedIn(TestUser),
        api = api,
        db = db,
        userRepository = userRepository,
        libraryItemPagingFactory = LibraryItemPagerFactory(
          remoteMediatorFactory = { _, _ -> mediator },
          db = db,
          libraryItemDao = dao,
          dispatcherProvider = dispatcherProvider,
        ),
        libraryItemDao = dao,
        urlHydrator = FakeUrlHydrator(),
        dispatcherProvider = dispatcherProvider,
      )
      block()
    } finally {
      driver.close()
    }
  }
}
