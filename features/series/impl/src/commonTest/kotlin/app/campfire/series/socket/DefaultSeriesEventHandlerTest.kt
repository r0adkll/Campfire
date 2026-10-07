// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.socket

import app.campfire.common.test.user
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.session.UserSession
import app.campfire.data.Series
import app.campfire.db.test.testDb
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers

class DefaultSeriesEventHandlerTest {

  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )

  @Test
  fun `a removed series only leaves this account's cache`() = testDb { db ->
    db.seriesQueries.insertOrIgnore(series(userId = "demo"))
    db.seriesQueries.insertOrIgnore(series(userId = "bob"))
    val handler = DefaultSeriesEventHandler(db, UserSession.LoggedIn(user("bob")), dispatchers)

    handler.onSeriesRemoved(seriesId = "series", libraryId = "library")

    assertThat(db.seriesQueries.selectByLibraryId("library", "demo").executeAsList().map { it.id })
      .containsExactly("series")
    assertThat(db.seriesQueries.selectByLibraryId("library", "bob").executeAsList()).isEmpty()
  }

  private fun series(userId: String) = Series(
    id = "series",
    name = "The Stormlight Archive",
    description = null,
    addedAt = 0,
    updatedAt = 0,
    inProgress = false,
    hasActiveBook = false,
    hideFromContinueListening = false,
    bookInProgressLastUpdate = null,
    firstBookUnreadId = null,
    libraryId = "library",
    userId = userId,
  )
}
