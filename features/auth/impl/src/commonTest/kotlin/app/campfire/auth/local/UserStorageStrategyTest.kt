// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.local

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.network.models.AudioBookmark
import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User
import app.campfire.network.models.UserPermissions
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest

class UserStorageStrategyTest {

  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )

  @Test
  fun `a second user on the same server is added alongside the first`() = accountsTest { db ->
    val strategy = NewUserStorageStrategy(db, dispatchers)

    strategy.store(SERVER_NAME, SERVER_URL, serverSettings(), user("user-1", "demo"), "lib-1")
    strategy.store(SERVER_NAME, SERVER_URL, serverSettings(), user("user-2", "bob"), "lib-1")

    val accounts = db.serversQueries.selectAll().executeAsList()
    assertThat(accounts.map { it.userId to it.url }).containsExactlyInAnyOrder(
      "user-1" to SERVER_URL,
      "user-2" to SERVER_URL,
    )
  }

  @Test
  fun `signing in again to an account on the device keeps its data`() = accountsTest { db ->
    val strategy = NewUserStorageStrategy(db, dispatchers)
    strategy.store(SERVER_NAME, SERVER_URL, serverSettings(), user("user-1", "demo", bookmarked = true), "lib-1")
    db.usersQueries.updateSelectedLibrary(libraryId = "lib-2", userId = "user-1")

    strategy.store("Renamed", SERVER_URL, serverSettings(version = "2.40.0"), user("user-1", "demo"), "lib-1")

    assertThat(db.bookmarksQueries.selectForUser("user-1").executeAsList().map { it.title })
      .containsExactly("Kept")
    val account = db.serversQueries.selectByUserId("user-1").executeAsOne()
    assertThat(account.name).isEqualTo("Renamed")
    assertThat(account.version).isEqualTo("2.40.0")
    assertThat(account.selectedLibraryId).isEqualTo("lib-1")
  }

  @Test
  fun `signing in again at another address moves the account there`() = accountsTest { db ->
    val strategy = NewUserStorageStrategy(db, dispatchers)
    strategy.store(SERVER_NAME, SERVER_URL, serverSettings(), user("user-1", "demo"), "lib-1")

    strategy.store(SERVER_NAME, OTHER_URL, serverSettings(), user("user-1", "demo"), "lib-1")

    val account = db.serversQueries.selectByUserId("user-1").executeAsOne()
    assertThat(account.url).isEqualTo(OTHER_URL)
    assertThat(account.serverUrl).isEqualTo(OTHER_URL)
  }

  @Test
  fun `re-authenticating refreshes the account in place`() = accountsTest { db ->
    NewUserStorageStrategy(db, dispatchers)
      .store(SERVER_NAME, SERVER_URL, serverSettings(), user("user-1", "demo", bookmarked = true), "lib-1")
    db.usersQueries.updateSelectedLibrary(libraryId = "lib-2", userId = "user-1")

    ExistingUserStorageStrategy(db, dispatchers)
      .store("Renamed", SERVER_URL, serverSettings(), user("user-1", "demo-renamed"), "lib-1")

    val account = db.serversQueries.selectByUserId("user-1").executeAsOne()
    assertThat(account.name).isEqualTo("Renamed")
    assertThat(account.name_).isEqualTo("demo-renamed")
    assertThat(account.selectedLibraryId).isEqualTo("lib-2")
    assertThat(db.bookmarksQueries.selectForUser("user-1").executeAsList().map { it.title })
      .containsExactly("Kept")
  }

  /** An in-memory database with foreign keys enforced, as on Android, so cascading deletes run. */
  private fun accountsTest(block: suspend (CampfireDatabase) -> Unit) = runTest {
    val driver = createDriver()
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    try {
      block(DatabaseFactory(driver).build())
    } finally {
      driver.close()
    }
  }

  private fun user(id: String, username: String, bookmarked: Boolean = false) = User(
    id = id,
    username = username,
    type = "user",
    mediaProgress = emptyList(),
    seriesHideFromContinueListening = emptyList(),
    bookmarks = if (bookmarked) {
      listOf(AudioBookmark(libraryItemId = "li-1", title = "Kept", time = 42f, createdAt = 0L))
    } else {
      emptyList()
    },
    isActive = true,
    isLocked = false,
    lastSeen = null,
    createdAt = 0L,
    permissions = UserPermissions(
      download = true,
      update = false,
      delete = false,
      upload = false,
      accessAllLibraries = true,
      accessAllTags = true,
      accessExplicitContent = true,
    ),
    librariesAccessible = emptyList(),
  )

  private fun serverSettings(version: String = "2.36.0") = ServerSettings(
    id = "server-settings",
    scannerFindCovers = false,
    scannerCoverProvider = "google",
    scannerParseSubtitle = false,
    scannerPreferMatchedMetadata = false,
    scannerDisableWatcher = false,
    storeCoverWithItem = false,
    storeMetadataWithItem = false,
    metadataFileFormat = "json",
    rateLimitLoginRequests = 10,
    rateLimitLoginWindow = 600000L,
    backupSchedule = "30 1 * * *",
    backupsToKeep = 2,
    maxBackupSize = 1,
    loggerDailyLogsToKeep = 7,
    loggerScannerLogsToKeep = 2,
    homeBookshelfView = 1,
    bookshelfView = 1,
    sortingIgnorePrefix = false,
    sortingPrefixes = listOf("the"),
    chromecastEnabled = false,
    dateFormat = "MM/dd/yyyy",
    timeFormat = "HH:mm",
    language = "en-us",
    logLevel = 2,
    version = version,
  )

  private companion object {
    const val SERVER_NAME = "Home"
    const val SERVER_URL = "https://abs.example.com"
    const val OTHER_URL = "https://abs.lan:13378"
  }
}
