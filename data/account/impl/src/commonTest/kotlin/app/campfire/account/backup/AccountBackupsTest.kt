// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.backup

import app.campfire.account.FakeServerDao
import app.campfire.account.api.AbsToken
import app.campfire.account.api.BackedUpAccount
import app.campfire.account.api.TokenBackup
import app.campfire.account.storage.SecureExtraHeaderStorage
import app.campfire.account.storage.SecureTokenStorage
import app.campfire.account.testServer
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.UserId
import app.campfire.settings.test.TestPrivacySettings
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class AccountBackupsTest {

  private val tokenBackup = FakeTokenBackup()
  private val servers = MutableStateFlow(listOf(testServer("alice"), testServer("bob")))

  @Test
  fun `backing up keeps the account's current sign-in`() = runTest {
    val backups = backups()
    tokens().put("alice", AbsToken("access-2", "refresh-2"))
    headers().put("alice", mapOf("X-Gate" to "open"))

    backups.backUp("alice")

    assertThat(tokenBackup.kept).containsExactly(
      BackedUpAccount(
        serverUrl = "https://abs.example.com",
        serverName = "Home",
        userId = "alice",
        token = AbsToken("access-2", "refresh-2"),
        extraHeaders = mapOf("X-Gate" to "open"),
      ),
    )
  }

  @Test
  fun `nothing is kept once the user turns it off`() = runTest {
    val backups = backups(keepSignedIn = false)
    tokens().put("alice", AbsToken("access", "refresh"))

    backups.backUp("alice")

    assertThat(tokenBackup.kept).isEmpty()
  }

  @Test
  fun `an account without a token isn't kept`() = runTest {
    backups().backUp("alice")

    assertThat(tokenBackup.kept).isEmpty()
  }

  @Test
  fun `syncing keeps every signed in account`() = runTest {
    val backups = backups()
    tokens().put("alice", AbsToken("access-a", "refresh-a"))
    tokens().put("bob", AbsToken("access-b", "refresh-b"))

    backups.sync()

    assertThat(tokenBackup.kept.map { it.userId }).containsExactly("alice", "bob")
  }

  @Test
  fun `syncing after turning it off clears the backup`() = runTest {
    tokenBackup.put(BackedUpAccount("https://abs.example.com", "Home", "alice", AbsToken("a", "r"), emptyMap()))

    backups(keepSignedIn = false).sync()

    assertThat(tokenBackup.kept).isEmpty()
  }

  @Test
  fun `forgetting removes the account even when turned off`() = runTest {
    tokenBackup.put(BackedUpAccount("https://abs.example.com", "Home", "alice", AbsToken("a", "r"), emptyMap()))

    backups(keepSignedIn = false).forget("alice")

    assertThat(tokenBackup.kept).isEmpty()
  }

  private val tokenSettings = MapSettings()
  private val headerSettings = MapSettings()

  private fun TestScope.tokens() = SecureTokenStorage(tokenSettings, asTestDispatcherProvider())
  private fun TestScope.headers() = SecureExtraHeaderStorage(headerSettings, asTestDispatcherProvider())

  private fun TestScope.backups(keepSignedIn: Boolean = true) = AccountBackups(
    tokenBackup = tokenBackup,
    tokenStorage = tokens(),
    extraHeaderStorage = headers(),
    serverDao = FakeServerDao(servers),
    privacySettings = TestPrivacySettings(backgroundScope).apply { keepSignedInAfterReinstall = keepSignedIn },
  )

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
