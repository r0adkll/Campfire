// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.backup

import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.account.api.BackedUpAccount
import app.campfire.account.api.RestorableAccount
import app.campfire.account.api.RestorableAccountRepository
import app.campfire.account.api.ServerRepository
import app.campfire.account.api.TokenBackup
import app.campfire.auth.api.AuthException
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.api.model.ServerStatus
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.Server
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class DefaultBackedUpAccountRestorerTest {

  private val tokenBackup = FakeTokenBackup()
  private val servers = FakeServerRepository()
  private val accountManager = FakeAccountManager()
  private val authRepository = FakeAuthRepository(servers)

  @Test
  fun `kept accounts are restored and the most recently used one becomes active`() = runTest {
    tokenBackup.kept += listOf(account("alice"), account("bob"))

    restorer(restorableOrder = listOf("bob", "alice")).restoreAll()

    // Bob was used last, so he goes first and ends up active
    assertThat(authRepository.restored).containsExactly("bob" to false, "alice" to false)
    assertThat(accountManager.switchedTo).isEqualTo("bob")
  }

  @Test
  fun `accounts already signed in aren't restored again`() = runTest {
    tokenBackup.kept += listOf(account("alice"), account("bob"))
    servers.all += server("alice")

    restorer().restoreAll()

    assertThat(authRepository.restored).containsExactly("bob" to false)
  }

  @Test
  fun `a rejected token is dropped from the backup`() = runTest {
    tokenBackup.kept += listOf(account("alice"), account("bob"))
    authRepository.failures["alice"] = AuthException.InvalidCredentials()

    restorer(restorableOrder = listOf("alice", "bob")).restoreAll()

    assertThat(tokenBackup.kept.map { it.userId }).containsOnly("bob")
    // Alice failed, so the first account that came back is active instead
    assertThat(accountManager.switchedTo).isEqualTo("bob")
  }

  @Test
  fun `an account that fails offline stays kept for the next launch`() = runTest {
    tokenBackup.kept += account("alice")
    authRepository.failures["alice"] = AuthException.Network(IOException("offline"))

    restorer().restoreAll()

    assertThat(tokenBackup.kept.map { it.userId }).containsOnly("alice")
    assertThat(accountManager.switchedTo).isNull()
  }

  @Test
  fun `nothing is restored where there's no backup`() = runTest {
    tokenBackup.isAvailable = false
    tokenBackup.kept += account("alice")

    restorer().restoreAll()

    assertThat(authRepository.restored).isEmpty()
  }

  @Test
  fun `nobody is restoring once it's done`() = runTest {
    tokenBackup.kept += account("alice")
    val restorer = restorer()

    restorer.restoreAll()

    assertThat(restorer.restoring.value).isEmpty()
  }

  private fun kotlinx.coroutines.test.TestScope.restorer(
    restorableOrder: List<UserId> = emptyList(),
  ) = DefaultBackedUpAccountRestorer(
    tokenBackup = tokenBackup,
    authRepository = authRepository,
    serverRepository = servers,
    accountManager = accountManager,
    restorableAccountRepository = object : RestorableAccountRepository {
      override fun observeRestorableAccounts(): Flow<List<RestorableAccount>> =
        flowOf(restorableOrder.map { RestorableAccount(SERVER_URL, "Home", it, it) })
      override suspend fun dismiss(account: RestorableAccount) = Unit
    },
    applicationScope = backgroundScope,
  )

  private class FakeTokenBackup : TokenBackup {
    override var isAvailable: Boolean = true
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

  private class FakeServerRepository : ServerRepository {
    val all = mutableListOf<Server>()
    override fun observeCurrentServer(): Flow<Server> = emptyFlow()
    override fun observeAllServers(): Flow<List<Server>> = flowOf(all)
    override suspend fun getCurrentServer(): Server? = null
    override suspend fun getAllServers(): List<Server> = all.toList()
    override suspend fun changeName(newName: String) = Unit
    override suspend fun remove(server: Server) = Unit
  }

  /** Restoring adds the account's server, like the real sign-in path stores it */
  private class FakeAuthRepository(private val servers: FakeServerRepository) : AuthRepository {
    val restored = mutableListOf<Pair<UserId, Boolean>>()
    val failures = mutableMapOf<UserId, Throwable>()

    override suspend fun restore(account: BackedUpAccount, activate: Boolean): Result<Unit> {
      restored += account.userId to activate
      failures[account.userId]?.let { return Result.failure(it) }
      servers.all += server(account.userId)
      return Result.success(Unit)
    }

    override suspend fun status(serverUrl: String, networkSettings: NetworkSettings?): Result<ServerStatus> =
      error("Unused")
    override suspend fun authenticate(
      serverUrl: String,
      serverName: String,
      username: String,
      password: String,
      userId: UserId?,
      networkSettings: NetworkSettings?,
    ): Result<Unit> = error("Unused")
    override suspend fun authenticate(
      serverUrl: String,
      serverName: String,
      codeVerifier: String,
      code: String,
      state: String,
      userId: UserId?,
      networkSettings: NetworkSettings?,
    ): Result<Unit> = error("Unused")
    override suspend fun getNetworkSettings(userId: UserId): NetworkSettings? = null
  }

  private class FakeAccountManager : AccountManager {
    var switchedTo: UserId? = null
    override suspend fun switchAccount(user: User) {
      switchedTo = user.id
    }
    override suspend fun addAccount(
      serverUrl: String,
      accessToken: String,
      refreshToken: String?,
      extraHeaders: Map<String, String>?,
      user: User,
      activate: Boolean,
    ) = Unit
    override suspend fun invalidateAccount(user: User) = Unit
    override suspend fun logout(server: Server) = Unit
    override suspend fun getToken(userId: UserId): AbsToken? = null
    override suspend fun updateToken(userId: UserId, newToken: AbsToken) = Unit
    override suspend fun getExtraHeaders(userId: UserId): Map<String, String>? = null
    override suspend fun setExtraHeaders(userId: UserId, headers: Map<String, String>) = Unit
    override fun observeExtraHeaders(userId: UserId): Flow<Map<String, String>> = emptyFlow()
  }

  private companion object {
    const val SERVER_URL = "https://abs.example.com"

    fun account(userId: UserId) = BackedUpAccount(
      serverUrl = SERVER_URL,
      serverName = "Home",
      userId = userId,
      token = AbsToken("access-$userId", "refresh-$userId"),
      extraHeaders = emptyMap(),
    )

    fun server(userId: UserId) = Server(
      url = SERVER_URL,
      name = "Home",
      user = User(
        id = userId,
        name = userId,
        selectedLibraryId = "library",
        type = User.Type.User,
        isActive = true,
        isLocked = false,
        lastSeen = 0L,
        createdAt = 0L,
        permissions = User.Permissions(
          download = true,
          update = false,
          delete = false,
          upload = false,
          accessAllLibraries = true,
          accessAllTags = true,
          accessExplicitContent = true,
        ),
        serverUrl = SERVER_URL,
      ),
      settings = Server.Settings(
        scannerFindCovers = false,
        scannerCoverProvider = "",
        scannerParseSubtitle = false,
        scannerPreferMatchedMetadata = false,
        scannerDisableWatcher = false,
        storeCoverWithItem = false,
        storeMetadataWithItem = false,
        metadataFileFormat = "",
        rateLimitLoginRequests = 0,
        rateLimitLoginWindow = 0,
        backupSchedule = "",
        backupsToKeep = 0,
        maxBackupSize = 0,
        loggerDailyLogsToKeep = 0,
        loggerScannerLogsToKeep = 0,
        homeBookshelfView = 0,
        bookshelfView = 0,
        sortingIgnorePrefix = false,
        sortingPrefixes = emptyList(),
        chromecastEnabled = false,
        dateFormat = "",
        timeFormat = "",
        language = "",
        logLevel = 0,
        version = "",
      ),
    )
  }
}
