// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists

import app.campfire.CampfireDatabase
import app.campfire.account.api.UserSessionManager
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.Playlist
import app.campfire.core.session.UserSession
import app.campfire.core.time.GrandFatherTime
import app.campfire.data.mapping.dao.SqlDelightLibraryItemDao
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.network.models.PlaylistExpanded
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.playlists.store.PlaylistsStore
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

class StorePlaylistsRepositoryTest {

  private val api = FakeAudioBookShelfApi()
  private val testUser = user(TestUserId)
  private val userRepository = FakeUserRepository().apply {
    currentUser = testUser
    currentUserFlow.tryEmit(testUser)
  }

  @Test
  fun `refresh fetches the selected library's playlists and saves them`() = playlistsTest { db ->
    api.playlistsResult = { Result.success(listOf(playlist("1", "Road Trip"))) }

    repository(db).refreshPlaylists()

    assertThat(api.playlistsRequests).containsExactly(testUser.selectedLibraryId)
    assertThat(db.playlistNames()).containsExactly("Road Trip")
  }

  @Test
  fun `refresh reaches playlists that are already being observed`() = playlistsTest { db ->
    val repository = repository(db)
    api.playlistsResult = { Result.success(listOf(playlist("1", "Road Trip"))) }

    repository.observeAllPlaylists().test {
      awaitPlaylistNames("Road Trip")

      api.playlistsResult = { Result.success(listOf(playlist("2", "Bedtime"))) }
      repository.refreshPlaylists()

      awaitPlaylistNames("Bedtime")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a failed refresh keeps the cached playlists`() = playlistsTest { db ->
    val repository = repository(db)
    api.playlistsResult = { Result.success(listOf(playlist("1", "Road Trip"))) }
    repository.refreshPlaylists()

    api.playlistsResult = { Result.failure(IllegalStateException("Unable to resolve host")) }
    repository.refreshPlaylists()

    assertThat(api.playlistsRequests).containsExactly(
      testUser.selectedLibraryId,
      testUser.selectedLibraryId,
    )
    assertThat(db.playlistNames()).containsExactly("Road Trip")
  }

  private fun TestScope.repository(db: CampfireDatabase): StorePlaylistsRepository {
    val dispatcherProvider = asTestDispatcherProvider()
    val urlHydrator = FakeUrlHydrator()
    val userSession = UserSession.LoggedIn(testUser)
    val libraryItemDao = SqlDelightLibraryItemDao(db, urlHydrator, dispatcherProvider, userSession)
    return StorePlaylistsRepository(
      userRepository = userRepository,
      db = db,
      urlHydrator = urlHydrator,
      libraryItemDao = libraryItemDao,
      storeFactory = PlaylistsStore.Factory(
        api = api,
        db = db,
        libraryItemDao = libraryItemDao,
        urlHydrator = urlHydrator,
        fatherTime = GrandFatherTime,
        userSessionManager = FixedUserSessionManager(userSession),
        dispatcherProvider = dispatcherProvider,
      ),
      dispatcherProvider = dispatcherProvider,
    )
  }

  private suspend fun CampfireDatabase.playlistNames(): List<String> {
    return playlistsQueries.selectByLibraryId(testUser.selectedLibraryId, testUser.id).awaitAsList().map { it.name }
  }

  /** Skips emissions until the playlists are exactly [names], in order. */
  private suspend fun ReceiveTurbine<List<Playlist>>.awaitPlaylistNames(vararg names: String) {
    while (true) {
      if (awaitItem().map { it.name } == names.toList()) return
    }
  }

  private fun playlistsTest(block: suspend TestScope.(CampfireDatabase) -> Unit) = runTest {
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

    fun playlist(id: String, name: String) = PlaylistExpanded(
      id = id,
      name = name,
      description = null,
      lastUpdate = 0L,
      createdAt = 0L,
      items = emptyList(),
    )
  }
}
