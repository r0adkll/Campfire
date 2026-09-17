// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home

import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.dao.SqlDelightLibraryItemDao
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.model.Shelf
import app.campfire.home.progress.MediaProgressDataSource
import app.campfire.home.store.home.HomeStore
import app.campfire.home.store.shelf.ShelfStore
import app.campfire.network.models.Shelf as NetworkShelf
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.test.FakeUserRepository
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class StoreHomeRepositoryTest {

  private val api = FakeAudioBookShelfApi()
  private val testUser = user(TestUserId)
  private val userRepository = FakeUserRepository().apply {
    currentUser = testUser
    currentUserFlow.tryEmit(testUser)
  }

  @Test
  fun `refresh fetches the selected library's feed and saves its shelves`() = homeTest { db ->
    api.personalizedHomeResult = { Result.success(listOf(shelf("authors", "Newest Authors"))) }

    repository(db).refreshHomeFeed()

    assertThat(api.personalizedHomeRequests).containsExactly(testUser.selectedLibraryId)
    assertThat(db.shelfLabels()).containsExactly("Newest Authors")
  }

  @Test
  fun `refresh reaches a home feed that is already being observed`() = homeTest { db ->
    val repository = repository(db)
    api.personalizedHomeResult = { Result.success(listOf(shelf("authors", "Newest Authors"))) }

    repository.observeHomeFeed().test {
      awaitShelfLabels("Newest Authors")

      api.personalizedHomeResult = {
        Result.success(listOf(shelf("authors", "Newest Authors"), shelf("series", "Recent Series")))
      }
      repository.refreshHomeFeed()

      awaitShelfLabels("Newest Authors", "Recent Series")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a failed refresh keeps the cached shelves`() = homeTest { db ->
    val repository = repository(db)
    api.personalizedHomeResult = { Result.success(listOf(shelf("authors", "Newest Authors"))) }
    repository.refreshHomeFeed()

    api.personalizedHomeResult = { Result.failure(IllegalStateException("Unable to resolve host")) }
    repository.refreshHomeFeed()

    assertThat(api.personalizedHomeRequests).containsExactly(
      testUser.selectedLibraryId,
      testUser.selectedLibraryId,
    )
    assertThat(db.shelfLabels()).containsExactly("Newest Authors")
  }

  private fun TestScope.repository(db: CampfireDatabase): StoreHomeRepository {
    val dispatcherProvider = asTestDispatcherProvider()
    val urlHydrator = FakeUrlHydrator()
    val userSession = UserSession.LoggedIn(testUser)
    return StoreHomeRepository(
      userRepository = userRepository,
      mediaProgressDataSource = NoMediaProgress,
      homeStoreFactory = HomeStore.Factory(api, db, urlHydrator, dispatcherProvider),
      shelfStoreFactory = ShelfStore.Factory(
        db = db,
        libraryItemDao = SqlDelightLibraryItemDao(db, urlHydrator, dispatcherProvider, userSession),
        urlHydrator = urlHydrator,
        dispatcherProvider = dispatcherProvider,
        userSession = userSession,
      ),
    )
  }

  private suspend fun CampfireDatabase.shelfLabels(): List<String> {
    return shelfQueries.select(testUser.selectedLibraryId, testUser.id).awaitAsList().map { it.label }
  }

  /** Skips emissions until the feed shows exactly [labels], in order. */
  private suspend fun ReceiveTurbine<FeedResponse<List<Shelf>>>.awaitShelfLabels(
    vararg labels: String,
  ) {
    while (true) {
      val shelves = awaitItem().dataOrNull ?: continue
      if (shelves.map { it.label } == labels.toList()) return
    }
  }

  private fun homeTest(block: suspend TestScope.(CampfireDatabase) -> Unit) = runTest {
    val driver = createDriver()
    try {
      block(DatabaseFactory(driver).build())
    } finally {
      driver.close()
    }
  }

  private object NoMediaProgress : MediaProgressDataSource {
    override fun observeMediaProgress(ids: Set<LibraryItemId>): Flow<Map<LibraryItemId, MediaProgress>> = emptyFlow()
  }

  private companion object {
    const val TestUserId = "user"

    fun shelf(id: String, label: String) = NetworkShelf.AuthorShelf(
      id = id,
      label = label,
      labelStringKey = id,
      total = 0,
      entities = emptyList(),
    )
  }
}
