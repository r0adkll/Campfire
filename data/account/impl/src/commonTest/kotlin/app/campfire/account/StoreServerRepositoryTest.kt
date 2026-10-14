// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account

import app.campfire.account.server.db.ServerDao
import app.campfire.account.server.store.ServerStore
import app.campfire.account.session.DefaultUserSessionManager
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.Server
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.test.runTest

class StoreServerRepositoryTest {

  private val dao = MutableServerDao(testServer("user-1"), testServer("user-2", url = "https://abs.other.com"))

  @Test
  fun `a removed server is gone from every server`() = runTest {
    val driver = createDriver()
    try {
      val repository = StoreServerRepository(
        db = DatabaseFactory(driver).build(),
        userSessionManager = DefaultUserSessionManager(),
        serverStoreFactory = ServerStore.Factory(dao),
        serverDao = dao,
        dispatcherProvider = asTestDispatcherProvider(),
      )
      // Something on screen watched the list before signing out, as the account switcher does
      repository.observeAllServers().first()

      repository.remove(testServer("user-1"))

      assertThat(repository.getAllServers().map { it.user.id }).containsExactly("user-2")
      repository.remove(testServer("user-2"))
      assertThat(repository.getAllServers()).isEmpty()
    } finally {
      driver.close()
    }
  }

  private class MutableServerDao(vararg servers: Server) : ServerDao {
    private val servers = MutableStateFlow(servers.toList())

    override fun observeOne(userId: String): Flow<Server> =
      servers.mapNotNull { all -> all.firstOrNull { it.user.id == userId } }

    override fun observeAll(): Flow<List<Server>> = servers

    override suspend fun delete(userId: String) {
      servers.value = servers.value.filterNot { it.user.id == userId }
    }
  }
}
