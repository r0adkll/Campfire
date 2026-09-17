// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.collections

import app.campfire.CampfireDatabase
import app.campfire.account.api.UserSessionManager
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.collections.store.CollectionsStore
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.Collection
import app.campfire.core.session.UserSession
import app.campfire.core.time.GrandFatherTime
import app.campfire.data.mapping.dao.SqlDelightLibraryItemDao
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.network.models.Collection as NetworkCollection
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.test.FakeUserRepository
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class StoreCollectionsRepositoryTest {

  private val api = FakeAudioBookShelfApi()
  private val testUser = user(TestUserId)
  private val userRepository = FakeUserRepository().apply {
    currentUser = testUser
    currentUserFlow.tryEmit(testUser)
  }

  @Test
  fun `refresh fetches the selected library's collections and saves them`() = collectionsTest { db ->
    api.collectionsResult = { libraryId -> Result.success(listOf(collection("1", "Classics", libraryId))) }

    repository(db).refreshCollections()

    assertThat(api.collectionsRequests).containsExactly(testUser.selectedLibraryId)
    assertThat(db.collectionNames()).containsExactly("Classics")
  }

  @Test
  fun `refresh reaches collections that are already being observed`() = collectionsTest { db ->
    val repository = repository(db)
    api.collectionsResult = { libraryId -> Result.success(listOf(collection("1", "Classics", libraryId))) }

    repository.observeAllCollections().test {
      awaitCollectionNames("Classics")

      api.collectionsResult = { libraryId -> Result.success(listOf(collection("2", "Sci-Fi", libraryId))) }
      repository.refreshCollections()

      awaitCollectionNames("Sci-Fi")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a failed refresh keeps the cached collections`() = collectionsTest { db ->
    val repository = repository(db)
    api.collectionsResult = { libraryId -> Result.success(listOf(collection("1", "Classics", libraryId))) }
    repository.refreshCollections()

    api.collectionsResult = { Result.failure(IllegalStateException("Unable to resolve host")) }
    repository.refreshCollections()

    assertThat(api.collectionsRequests).containsExactly(
      testUser.selectedLibraryId,
      testUser.selectedLibraryId,
    )
    assertThat(db.collectionNames()).containsExactly("Classics")
  }

  private fun TestScope.repository(db: CampfireDatabase): StoreCollectionsRepository {
    val dispatcherProvider = asTestDispatcherProvider()
    val urlHydrator = FakeUrlHydrator()
    val userSession = UserSession.LoggedIn(testUser)
    return StoreCollectionsRepository(
      userRepository = userRepository,
      db = db,
      urlHydrator = urlHydrator,
      storeFactory = CollectionsStore.Factory(
        api = api,
        db = db,
        libraryItemDao = SqlDelightLibraryItemDao(db, urlHydrator, dispatcherProvider, userSession),
        urlHydrator = urlHydrator,
        fatherTime = GrandFatherTime,
        userSessionManager = FixedUserSessionManager(userSession),
        dispatcherProvider = dispatcherProvider,
      ),
      dispatcherProvider = dispatcherProvider,
    )
  }

  private suspend fun CampfireDatabase.collectionNames(): List<String> {
    return collectionsQueries.selectByLibraryId(testUser.selectedLibraryId, testUser.id).awaitAsList().map { it.name }
  }

  /** Skips emissions until the collections are exactly [names], in order. */
  private suspend fun ReceiveTurbine<List<Collection>>.awaitCollectionNames(vararg names: String) {
    while (true) {
      if (awaitItem().map { it.name } == names.toList()) return
    }
  }

  private fun collectionsTest(block: suspend TestScope.(CampfireDatabase) -> Unit) = runTest {
    val driver = createDriver()
    try {
      block(DatabaseFactory(driver).build())
    } finally {
      driver.close()
    }
  }

  private class FixedUserSessionManager(session: UserSession) : UserSessionManager {
    private val session = MutableStateFlow(session)
    override var current: UserSession
      get() = session.value
      set(value) {
        session.value = value
      }

    override fun observe(): StateFlow<UserSession> = session
  }

  private companion object {
    const val TestUserId = "user"

    fun collection(id: String, name: String, libraryId: String) = NetworkCollection(
      id = id,
      libraryId = libraryId,
      name = name,
      description = null,
      books = emptyList(),
      lastUpdate = 0L,
      createdAt = 0L,
    )
  }
}
