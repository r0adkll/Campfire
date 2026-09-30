// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.restore

import app.campfire.account.FakeServerDao
import app.campfire.account.api.RestorableAccount
import app.campfire.account.testServer
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.Server
import app.campfire.settings.test.TestCampfireSettings
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class AccountRestoreStoreTest {

  private val settings = MapSettings()
  private val servers = MutableStateFlow<List<Server>>(emptyList())

  @Test
  fun `accounts from a previous install are offered after a restore`() = runTest {
    // The previous install mirrors its account into the backed-up settings
    val previousInstall = store()
    val mirroring = backgroundScope.launch { previousInstall.mirror() }
    servers.value = listOf(testServer(url = SERVER_URL, userId = "user-1", userName = "alice"))
    testScheduler.runCurrent()
    mirroring.cancel()

    // The reinstall restores the settings, but not the database
    servers.value = emptyList()
    val reinstall = store()
    backgroundScope.launch { reinstall.mirror() }
    testScheduler.runCurrent()

    reinstall.observeRestorableAccounts().test {
      assertThat(awaitItem()).containsExactly(
        RestorableAccount(
          serverUrl = SERVER_URL,
          serverName = "Home",
          userId = "user-1",
          userName = "alice",
        ),
      )

      // Signing back in stops offering it
      servers.value = listOf(testServer(url = SERVER_URL, userId = "user-1", userName = "alice"))
      assertThat(awaitItem()).isEmpty()
    }
  }

  @Test
  fun `logging out doesn't offer the account again`() = runTest {
    val store = store()
    backgroundScope.launch { store.mirror() }
    servers.value = listOf(testServer(url = SERVER_URL, userId = "user-1", userName = "alice"))
    testScheduler.runCurrent()

    store.forget("user-1")
    servers.value = emptyList()
    testScheduler.runCurrent()

    store.observeRestorableAccounts().test {
      assertThat(awaitItem()).isEmpty()
    }
  }

  @Test
  fun `an unreadable snapshot is discarded`() = runTest {
    settings.putString("account_restore_snapshot", "{not json")
    val store = store()
    backgroundScope.launch { store.mirror() }

    store.observeRestorableAccounts().test {
      assertThat(awaitItem()).isEmpty()
    }
  }

  private fun TestScope.store() = AccountRestoreStore(
    settings = settings,
    serverDao = FakeServerDao(servers),
    campfireSettings = TestCampfireSettings(backgroundScope),
    dispatcherProvider = asTestDispatcherProvider(),
  )

  private companion object {
    const val SERVER_URL = "https://abs.example.com"
  }
}
