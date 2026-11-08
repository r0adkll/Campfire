// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.server.db

import app.campfire.CampfireDatabase
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.data.SessionQueue
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.cash.sqldelight.async.coroutines.awaitAsList
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class SqlDelightServerDaoTest {

  @Test
  fun `deleting an account deletes its playback queue`() = daoTest { db ->
    db.queue("leaving", "book-a")
    db.queue("staying", "book-b")

    SqlDelightServerDao(db, asTestDispatcherProvider()).delete("leaving")

    assertThat(db.queuedItems("leaving")).isEmpty()
    assertThat(db.queuedItems("staying")).containsExactly("book-b")
  }

  private suspend fun CampfireDatabase.queue(userId: String, libraryItemId: String) {
    sessionQueueQueries.insert(
      SessionQueue(userId = userId, libraryItemId = libraryItemId, episodeId = "", queueIndex = 0),
    )
  }

  private suspend fun CampfireDatabase.queuedItems(userId: String): List<String> {
    return sessionQueueQueries.selectAll(userId).awaitAsList().map { it.libraryItemId }
  }

  private fun daoTest(block: suspend TestScope.(CampfireDatabase) -> Unit) = runTest {
    val driver = createDriver()
    try {
      block(DatabaseFactory(driver).build())
    } finally {
      driver.close()
    }
  }
}
