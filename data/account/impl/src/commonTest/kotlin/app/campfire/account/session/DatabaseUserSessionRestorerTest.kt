// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.session

import app.campfire.CampfireDatabase
import app.campfire.account.api.AbsToken
import app.campfire.account.storage.TokenStorage
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.asDatabaseModel
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User as NetworkUser
import app.campfire.network.models.UserPermissions
import app.campfire.settings.test.TestDeviceSettings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DatabaseUserSessionRestorerTest {

  private val deviceSettings = TestDeviceSettings()
  private val tokenStorage = UnreadableTokenStorage()

  @Test
  fun `signed out without a current user`() = restorerTest { db ->
    assertThat(restorer(db).restore()).isEqualTo(UserSession.LoggedOut)
  }

  @Test
  fun `signed out when the current user's account is gone`() = restorerTest { db ->
    deviceSettings.currentUserId = USER

    assertThat(restorer(db).restore()).isEqualTo(UserSession.LoggedOut)
    assertThat(deviceSettings.currentUserId).isNull()
  }

  @Test
  fun `signed in with a stored token without reading it`() = restorerTest { db ->
    db.insertAccount(USER)
    deviceSettings.currentUserId = USER
    tokenStorage.stored += USER

    assertThat(restorer(db).restore())
      .isInstanceOf<UserSession.LoggedIn>()
      .prop(UserSession.LoggedIn::user)
      .prop("id") { it.id }
      .isEqualTo(USER)
  }

  @Test
  fun `needs authentication without a stored token`() = restorerTest { db ->
    db.insertAccount(USER)
    deviceSettings.currentUserId = USER

    assertThat(restorer(db).restore())
      .isInstanceOf<UserSession.NeedsAuthentication>()
      .prop(UserSession.NeedsAuthentication::server)
      .prop("userId") { it.user.id }
      .isEqualTo(USER)
  }

  private fun TestScope.restorer(db: CampfireDatabase) = DatabaseUserSessionRestorer(
    tokenStorage = tokenStorage,
    deviceSettings = deviceSettings,
    db = db,
    dispatcherProvider = asTestDispatcherProvider(),
  )

  private fun restorerTest(block: suspend TestScope.(CampfireDatabase) -> Unit) = runTest {
    val driver = createDriver()
    try {
      block(DatabaseFactory(driver).build())
    } finally {
      driver.close()
    }
  }

  private suspend fun CampfireDatabase.insertAccount(userId: UserId) {
    serversQueries.insert(serverSettings().asDatabaseModel(url = SERVER_URL, userId = userId, name = "Home"))
    usersQueries.insert(networkUser(userId).asDatabaseModel(SERVER_URL, "library"))
  }

  /** Knows which users have a token, but fails a read: restoring must not decrypt one. */
  private class UnreadableTokenStorage : TokenStorage {
    val stored = mutableSetOf<UserId>()

    override suspend fun get(userId: UserId): AbsToken? = error("Restoring read the token of $userId")
    override suspend fun has(userId: UserId): Boolean = userId in stored
    override suspend fun put(userId: UserId, token: AbsToken) = error("Restoring wrote a token")
    override suspend fun remove(userId: UserId) = error("Restoring removed a token")
  }

  private fun networkUser(userId: UserId) = NetworkUser(
    id = userId,
    username = "listener",
    type = "user",
    mediaProgress = emptyList(),
    seriesHideFromContinueListening = emptyList(),
    bookmarks = emptyList(),
    isActive = true,
    isLocked = false,
    lastSeen = 0L,
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

  private fun serverSettings() = ServerSettings(
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
    version = "2.36.0",
  )

  private companion object {
    const val USER = "user-1"
    const val SERVER_URL = "https://abs.example.com"
  }
}
