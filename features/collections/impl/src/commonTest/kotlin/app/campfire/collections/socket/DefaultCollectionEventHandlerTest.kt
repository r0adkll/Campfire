// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.collections.socket

import app.campfire.common.test.user
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.session.UserSession
import app.campfire.data.Collections
import app.campfire.db.test.testDb
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers

class DefaultCollectionEventHandlerTest {

  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )

  @Test
  fun `a removed collection only leaves this account's cache`() = testDb { db ->
    db.collectionsQueries.insert(collection(userId = "demo"))
    db.collectionsQueries.insert(collection(userId = "bob"))
    val handler = DefaultCollectionEventHandler(db, UserSession.LoggedIn(user("bob")), dispatchers)

    handler.onCollectionRemoved("collection")

    assertThat(db.collectionsQueries.selectAll("demo").executeAsList().map { it.id }).containsExactly("collection")
    assertThat(db.collectionsQueries.selectAll("bob").executeAsList()).isEmpty()
  }

  private fun collection(userId: String) = Collections(
    id = "collection",
    name = "Favorites",
    description = null,
    cover = null,
    coverFullPath = null,
    updatedAt = 0,
    createdAt = 0,
    libraryId = "library",
    userId = userId,
  )
}
