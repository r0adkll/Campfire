// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.socket

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.data.mapping.asDatabaseModel
import app.campfire.db.test.testDb
import app.campfire.network.models.User as NetworkUser
import app.campfire.network.models.UserPermissions
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers

class DefaultUserEventHandlerTest {

  private val serverUrl = "https://abs.example.com"

  private fun networkUser(download: Boolean, lastSeen: Long) = NetworkUser(
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
  fun onUserUpdated_writesServerFieldsButKeepsLastSeen() = testDb { db ->
    db.usersQueries.insert(networkUser(download = true, lastSeen = 1_000L).asDatabaseModel(serverUrl, "lib-1"))
    val handler = DefaultUserEventHandler(
      db = db,
      dispatcherProvider = DispatcherProvider(
        io = Dispatchers.Default,
        databaseWrite = Dispatchers.Default,
        databaseRead = Dispatchers.Default,
        computation = Dispatchers.Default,
        main = Dispatchers.Default,
      ),
    )

    handler.onUserUpdated(networkUser(download = false, lastSeen = 2_000L))

    val row = db.usersQueries.selectForServer(serverUrl).executeAsOne()
    assertThat(row.permission_download).isEqualTo(false)
    assertThat(row.lastSeen).isEqualTo(1_000L)
  }
}
