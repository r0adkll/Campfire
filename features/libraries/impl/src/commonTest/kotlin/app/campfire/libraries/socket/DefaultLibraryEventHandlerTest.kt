// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.socket

import app.campfire.common.test.user
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.session.UserSession
import app.campfire.data.Library
import app.campfire.db.test.testDb
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers

class DefaultLibraryEventHandlerTest {

  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )

  @Test
  fun `a removed library only leaves this account's cache`() = testDb { db ->
    db.librariesQueries.insert(library(userId = "demo"))
    db.librariesQueries.insert(library(userId = "bob"))
    val handler = DefaultLibraryEventHandler(db, UserSession.LoggedIn(user("bob")), dispatchers)

    handler.onLibraryRemoved("library")

    assertThat(db.librariesQueries.selectAll("demo").executeAsList().map { it.id }).containsExactly("library")
    assertThat(db.librariesQueries.selectAll("bob").executeAsList()).isEmpty()
  }

  private fun library(userId: String) = Library(
    id = "library",
    name = "Audiobooks",
    displayOrder = 0,
    icon = "database",
    mediaType = "book",
    provider = "audible",
    createdAt = 0,
    lastUpdate = 0,
    coverAspectRatio = 1,
    audiobooksOnly = false,
    userId = userId,
  )
}
