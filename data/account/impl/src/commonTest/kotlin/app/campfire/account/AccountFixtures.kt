// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account

import app.campfire.account.server.db.ServerDao
import app.campfire.core.model.Server
import app.campfire.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class FakeServerDao(private val servers: Flow<List<Server>>) : ServerDao {
  override fun observeOne(userId: String): Flow<Server> = emptyFlow()
  override fun observeAll(): Flow<List<Server>> = servers
  override suspend fun delete(userId: String) = Unit
}

internal fun testServer(
  userId: String,
  userName: String = userId,
  url: String = "https://abs.example.com",
) = Server(
  url = url,
  name = "Home",
  user = User(
    id = userId,
    name = userName,
    selectedLibraryId = "library",
    type = User.Type.User,
    isActive = true,
    isLocked = false,
    lastSeen = 0L,
    createdAt = 0L,
    permissions = User.Permissions(
      download = true,
      update = false,
      delete = false,
      upload = false,
      accessAllLibraries = true,
      accessAllTags = true,
      accessExplicitContent = true,
    ),
    serverUrl = url,
  ),
  settings = Server.Settings(
    scannerFindCovers = false,
    scannerCoverProvider = "",
    scannerParseSubtitle = false,
    scannerPreferMatchedMetadata = false,
    scannerDisableWatcher = false,
    storeCoverWithItem = false,
    storeMetadataWithItem = false,
    metadataFileFormat = "",
    rateLimitLoginRequests = 0,
    rateLimitLoginWindow = 0,
    backupSchedule = "",
    backupsToKeep = 0,
    maxBackupSize = 0,
    loggerDailyLogsToKeep = 0,
    loggerScannerLogsToKeep = 0,
    homeBookshelfView = 0,
    bookshelfView = 0,
    sortingIgnorePrefix = false,
    sortingPrefixes = emptyList(),
    chromecastEnabled = false,
    dateFormat = "",
    timeFormat = "",
    language = "",
    logLevel = 0,
    version = "",
  ),
)
