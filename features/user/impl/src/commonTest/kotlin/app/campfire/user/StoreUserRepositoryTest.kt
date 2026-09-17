// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.User
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.asDatabaseModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.db.test.testDb
import app.campfire.network.models.User as NetworkUser
import app.campfire.network.models.UserPermissions
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.store.UserStore
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoreUserRepositoryTest {

  private val serverUrl = "https://abs.example.com"

  private fun networkUser(download: Boolean, lastSeen: Long = 0L) = NetworkUser(
    id = "user-1",
    username = "listener",
    type = "user",
    mediaProgress = emptyList(),
    seriesHideFromContinueListening = emptyList(),
    bookmarks = emptyList(),
    isActive = true,
    isLocked = false,
    lastSeen = lastSeen,
    createdAt = 0L,
    permissions = UserPermissions(
      download = download,
      update = false,
      delete = false,
      upload = false,
      accessAllLibraries = true,
      accessAllTags = true,
      accessExplicitContent = true,
    ),
    librariesAccessible = emptyList(),
  )

  @Test
  fun userFlow_picksUpPermissionChangesFromTheServer() = testDb { db ->
    // Signed in while downloads were allowed; an admin has since revoked them
    val signedIn = networkUser(download = true)
    db.usersQueries.insert(signedIn.asDatabaseModel(serverUrl, "lib-1"))
    val api = FakeAudioBookShelfApi().apply {
      currentUserResult = { Result.success(networkUser(download = false)) }
    }
    val session = UserSession.LoggedIn(signedIn.asDomainModel(serverUrl, "lib-1"))
    val scope = CoroutineScope(Job())
    val dispatchers = DispatcherProvider(
      io = Dispatchers.Default,
      databaseWrite = Dispatchers.Default,
      databaseRead = Dispatchers.Default,
      computation = Dispatchers.Default,
      main = Dispatchers.Default,
    )
    val repository = StoreUserRepository(
      userSession = session,
      userStoreFactory = UserStore.Factory(session, db, api, dispatchers),
      coroutineScopeHolder = CoroutineScopeHolder { scope },
    )

    assertThat(repository.userFlow.value.canDownload).isTrue()
    assertThat(repository.userFlow.first { !it.canDownload }.canDownload).isFalse()
    scope.cancel()
  }

  @Test
  fun userFlow_doesNotReEmitWhenTheServerOnlyBumpedLastSeen() = testDb { db ->
    // The server bumps lastSeen on every socket connection, so a refresh usually differs only there
    val signedIn = networkUser(download = true, lastSeen = 1_000L)
    db.usersQueries.insert(signedIn.asDatabaseModel(serverUrl, "lib-1"))
    val fetched = CompletableDeferred<Unit>()
    val api = FakeAudioBookShelfApi().apply {
      currentUserResult = {
        fetched.complete(Unit)
        Result.success(networkUser(download = true, lastSeen = 2_000L))
      }
    }
    val session = UserSession.LoggedIn(signedIn.asDomainModel(serverUrl, "lib-1"))
    val scope = CoroutineScope(Job())
    val dispatchers = DispatcherProvider(
      io = Dispatchers.Default,
      databaseWrite = Dispatchers.Default,
      databaseRead = Dispatchers.Default,
      computation = Dispatchers.Default,
      main = Dispatchers.Default,
    )
    val repository = StoreUserRepository(
      userSession = session,
      userStoreFactory = UserStore.Factory(session, db, api, dispatchers),
      coroutineScopeHolder = CoroutineScopeHolder { scope },
    )
    val userFlowEmissions = MutableStateFlow(emptyList<User>())
    val observedEmissions = MutableStateFlow(emptyList<User>())
    scope.launch { repository.userFlow.collect { user -> userFlowEmissions.update { it + user } } }
    scope.launch { repository.observeCurrentUser().collect { user -> observedEmissions.update { it + user } } }

    fetched.await()
    // Let the write land and any resulting emission propagate (real time: off the test scheduler)
    withContext(Dispatchers.Default) { delay(500) }

    assertThat(userFlowEmissions.value.map { it.lastSeen }).containsExactly(1_000L)
    assertThat(observedEmissions.value.map { it.lastSeen }).containsExactly(1_000L)
    scope.cancel()
  }

  @Test
  fun userFlow_keepsTheCachedUserWhenTheServerIsUnreachable() = testDb { db ->
    val signedIn = networkUser(download = true)
    db.usersQueries.insert(signedIn.asDatabaseModel(serverUrl, "lib-1"))
    val fetchAttempted = CompletableDeferred<Unit>()
    val api = FakeAudioBookShelfApi().apply {
      currentUserResult = {
        fetchAttempted.complete(Unit)
        Result.failure(IllegalStateException("offline"))
      }
    }
    val session = UserSession.LoggedIn(signedIn.asDomainModel(serverUrl, "lib-1"))
    val scope = CoroutineScope(Job())
    val dispatchers = DispatcherProvider(
      io = Dispatchers.Default,
      databaseWrite = Dispatchers.Default,
      databaseRead = Dispatchers.Default,
      computation = Dispatchers.Default,
      main = Dispatchers.Default,
    )
    val repository = StoreUserRepository(
      userSession = session,
      userStoreFactory = UserStore.Factory(session, db, api, dispatchers),
      coroutineScopeHolder = CoroutineScopeHolder { scope },
    )

    repository.userFlow.value
    fetchAttempted.await()
    // Give the failed response time to reach the shared flow (real time: the store runs off the test scheduler)
    withContext(Dispatchers.Default) { delay(500) }

    // The failed refresh neither crashes the shared flow nor drops the cached user
    assertThat(scope.coroutineContext[Job]!!.isActive).isTrue()
    assertThat(repository.userFlow.value.canDownload).isTrue()
    scope.cancel()
  }
}
