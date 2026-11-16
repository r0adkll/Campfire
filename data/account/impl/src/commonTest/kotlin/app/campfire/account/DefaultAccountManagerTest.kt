// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account

import app.campfire.account.api.AbsToken
import app.campfire.account.api.BackedUpAccount
import app.campfire.account.api.ServerRepository
import app.campfire.account.api.TokenBackup
import app.campfire.account.api.UserSessionManager
import app.campfire.account.api.di.UserGraphManager
import app.campfire.account.backup.AccountBackups
import app.campfire.account.server.LogoutUseCase
import app.campfire.account.storage.SecureExtraHeaderStorage
import app.campfire.account.storage.SecureTokenStorage
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.Server
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.settings.api.UserSettingsStore
import app.campfire.settings.test.TestDeviceSettings
import app.campfire.settings.test.TestPrivacySettings
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DefaultAccountManagerTest {

  private val alice = testServer("alice")
  private val bob = testServer("bob")

  private val servers = MutableStateFlow(listOf(alice))
  private val tokenBackup = FakeTokenBackup()
  private val tokenSettings = MapSettings()
  private val headerSettings = MapSettings()
  private val deviceSettings = TestDeviceSettings()
  private val sessions = RecordingUserSessionManager(UserSession.LoggedIn(alice.user))
  private val graphs = FakeUserGraphManager()
  private val userSettings = FakeUserSettingsStore()

  @Test
  fun `the account is gone by the time the logged out session is published`() = runTest {
    val manager = manager()
    signIn(alice)
    deviceSettings.currentUserId = "alice"

    // What anything reacting to the new session (the Welcome screen's restore) would find
    var tokenAtLogout: Boolean? = null
    var serversAtLogout: List<Server>? = null
    var keptAtLogout: List<BackedUpAccount>? = null
    sessions.onPublish = { session ->
      if (session == UserSession.LoggedOut) {
        tokenAtLogout = hasToken("alice")
        serversAtLogout = servers.value
        keptAtLogout = tokenBackup.kept.toList()
      }
    }

    manager.logout(alice)

    assertThat(sessions.published).containsExactly(UserSession.Loading, UserSession.LoggedOut)
    assertThat(tokenAtLogout).isEqualTo(false)
    assertThat(serversAtLogout).isNotNull().isEmpty()
    assertThat(keptAtLogout).isNotNull().isEmpty()
  }

  @Test
  fun `the account is removed after its graph is destroyed and before the next is created`() = runTest {
    val manager = manager()
    signIn(alice)
    deviceSettings.currentUserId = "alice"
    graphs.onDestroy = { graphs.events += "token=${hasToken("alice")}" }
    graphs.onCreate = { graphs.events += "token=${hasToken("alice")}" }

    manager.logout(alice)

    assertThat(graphs.events).containsExactly("destroy", "token=true", "create", "token=false")
  }

  @Test
  fun `logging out the current account switches to a remaining one`() = runTest {
    servers.value = listOf(alice, bob)
    val manager = manager()
    signIn(alice)
    signIn(bob)
    deviceSettings.currentUserId = "alice"

    manager.logout(alice)

    assertThat(sessions.current).isEqualTo(UserSession.LoggedIn(bob.user))
    assertThat(deviceSettings.currentUserId).isEqualTo("bob")
    assertThat(tokens().get("alice")).isNull()
    assertThat(tokens().get("bob")).isNotNull()
    assertThat(userSettings.cleared).containsExactly("alice")
  }

  @Test
  fun `logging out skips a remaining account that has no token`() = runTest {
    val carol = testServer("carol")
    servers.value = listOf(alice, bob, carol)
    val manager = manager()
    signIn(alice)
    signIn(carol)
    deviceSettings.currentUserId = "alice"

    manager.logout(alice)

    assertThat(sessions.current).isEqualTo(UserSession.LoggedIn(carol.user))
  }

  @Test
  fun `logging out asks for a sign in when no remaining account has a token`() = runTest {
    servers.value = listOf(alice, bob)
    val manager = manager()
    signIn(alice)
    deviceSettings.currentUserId = "alice"

    manager.logout(alice)

    assertThat(sessions.current).isEqualTo(UserSession.NeedsAuthentication(bob))
    // Remembered, so the next launch restores the sign-in prompt rather than the welcome screen
    assertThat(deviceSettings.currentUserId).isEqualTo("bob")
  }

  @Test
  fun `logging out the last account forgets the current user`() = runTest {
    val manager = manager()
    signIn(alice)
    deviceSettings.currentUserId = "alice"

    manager.logout(alice)

    assertThat(deviceSettings.currentUserId).isNull()
  }

  @Test
  fun `an account waiting to sign in again can be switched to once it has a token`() = runTest {
    val manager = manager()
    sessions.current = UserSession.NeedsAuthentication(alice)
    sessions.published.clear()
    deviceSettings.currentUserId = "alice"
    signIn(alice)

    manager.switchAccount(alice.user)

    assertThat(sessions.current).isEqualTo(UserSession.LoggedIn(alice.user))
  }

  @Test
  fun `switching to the signed in account does nothing`() = runTest {
    val manager = manager()
    signIn(alice)
    deviceSettings.currentUserId = "alice"

    manager.switchAccount(alice.user)

    assertThat(sessions.published).isEmpty()
    assertThat(graphs.events).isEmpty()
  }

  @Test
  fun `logging out another account leaves the session alone`() = runTest {
    servers.value = listOf(alice, bob)
    val manager = manager()
    signIn(alice)
    signIn(bob)
    deviceSettings.currentUserId = "alice"

    manager.logout(bob)

    assertThat(sessions.published).isEmpty()
    assertThat(graphs.events).isEmpty()
    assertThat(tokens().get("bob")).isNull()
    assertThat(servers.value).containsExactly(alice)
  }

  private suspend fun TestScope.signIn(server: Server) {
    tokens().put(server.user.id, AbsToken("access-${server.user.id}", "refresh-${server.user.id}"))
    backups().backUp(server.user.id)
  }

  private fun hasToken(userId: UserId) = tokenSettings.hasKey("accessToken_$userId")

  private fun TestScope.tokens() = SecureTokenStorage(tokenSettings, asTestDispatcherProvider())
  private fun TestScope.headers() = SecureExtraHeaderStorage(headerSettings, asTestDispatcherProvider())

  private fun TestScope.backups() = AccountBackups(
    tokenBackup = tokenBackup,
    tokenStorage = tokens(),
    extraHeaderStorage = headers(),
    serverDao = FakeServerDao(servers),
    privacySettings = TestPrivacySettings(backgroundScope).apply { keepSignedInAfterReinstall = true },
  )

  private fun TestScope.manager() = DefaultAccountManager(
    deviceSettings = deviceSettings,
    userSessionManager = sessions,
    tokenStorage = tokens(),
    extraHeaderStorage = headers(),
    logoutUseCase = object : LogoutUseCase {
      override suspend fun execute(server: Server) {
        servers.value = servers.value.filter { it.user.id != server.user.id }
      }
    },
    serverRepository = FakeServerRepository(servers),
    userGraphManager = graphs,
    accountBackups = backups(),
    userSettingsStore = userSettings,
    applicationScope = this,
  )

  private class RecordingUserSessionManager(initial: UserSession) : UserSessionManager {
    private val state = MutableStateFlow(initial)
    val published = mutableListOf<UserSession>()
    var onPublish: (UserSession) -> Unit = {}

    override var current: UserSession
      get() = state.value
      set(value) {
        state.value = value
        published += value
        onPublish(value)
      }

    override fun observe(): StateFlow<UserSession> = state
  }

  private class FakeUserGraphManager : UserGraphManager {
    val events = mutableListOf<String>()
    var onDestroy: () -> Unit = {}
    var onCreate: () -> Unit = {}

    override fun create(userSession: UserSession) {
      events += "create"
      onCreate()
    }

    override suspend fun destroy() {
      events += "destroy"
      onDestroy()
    }
  }

  private class FakeServerRepository(private val servers: MutableStateFlow<List<Server>>) : ServerRepository {
    override fun observeCurrentServer(): Flow<Server> = emptyFlow()
    override fun observeAllServers(): Flow<List<Server>> = servers
    override suspend fun getCurrentServer(): Server? = null
    override suspend fun getAllServers(): List<Server> = servers.value
    override suspend fun changeName(newName: String) = Unit
    override suspend fun remove(server: Server) = Unit
  }

  private class FakeUserSettingsStore : UserSettingsStore {
    val cleared = mutableListOf<UserId>()
    override fun clear(userId: UserId) {
      cleared += userId
    }
  }

  private class FakeTokenBackup : TokenBackup {
    override val isAvailable: Boolean = true
    val kept = mutableListOf<BackedUpAccount>()
    override suspend fun put(account: BackedUpAccount) {
      kept.removeAll { it.userId == account.userId }
      kept += account
    }
    override suspend fun remove(userId: UserId) {
      kept.removeAll { it.userId == userId }
    }
    override suspend fun clear() = kept.clear()
    override suspend fun getAll(): List<BackedUpAccount> = kept.toList()
  }
}
