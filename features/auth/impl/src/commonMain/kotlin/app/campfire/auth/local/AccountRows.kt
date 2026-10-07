// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.local

import app.campfire.CampfireDatabase
import app.campfire.core.model.User as DomainUser
import app.campfire.data.mapping.asDbModel
import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User

/**
 * Refreshes an account's server and user rows in place from a sign-in, and stores the progress and
 * bookmarks the sign-in returned. Call it inside a transaction, after the rows exist.
 */
internal suspend fun CampfireDatabase.updateAccount(
  serverName: String,
  serverUrl: String,
  serverSettings: ServerSettings,
  user: User,
) {
  serversQueries.update(
    url = serverUrl,
    name = serverName,
    scannerFindCovers = serverSettings.scannerFindCovers,
    scannerCoverProvider = serverSettings.scannerCoverProvider,
    scannerParseSubtitle = serverSettings.scannerParseSubtitle,
    scannerPreferMatchedMetadata = serverSettings.scannerPreferMatchedMetadata,
    scannerDisableWatcher = serverSettings.scannerDisableWatcher,
    storeCoverWithItem = serverSettings.storeCoverWithItem,
    storeMetadataWithItem = serverSettings.storeMetadataWithItem,
    metadataFileFormat = serverSettings.metadataFileFormat,
    rateLimitLoginRequests = serverSettings.rateLimitLoginRequests,
    rateLimitLoginWindow = serverSettings.rateLimitLoginWindow,
    backupSchedule = serverSettings.backupSchedule,
    backupsToKeep = serverSettings.backupsToKeep,
    maxBackupSize = serverSettings.maxBackupSize,
    loggerDailyLogsToKeep = serverSettings.loggerDailyLogsToKeep,
    loggerScannerLogsToKeep = serverSettings.loggerScannerLogsToKeep,
    homeBookshelfView = serverSettings.homeBookshelfView,
    bookshelfView = serverSettings.bookshelfView,
    sortingIgnorePrefix = serverSettings.sortingIgnorePrefix,
    sortingPrefixes = serverSettings.sortingPrefixes,
    chromecastEnabled = serverSettings.chromecastEnabled,
    dateFormat = serverSettings.dateFormat,
    timeFormat = serverSettings.timeFormat,
    language = serverSettings.language,
    logLevel = serverSettings.logLevel,
    version = serverSettings.version,
    userId = user.id,
  )

  usersQueries.update(
    id = user.id,
    serverUrl = serverUrl,
    name = user.username,
    type = DomainUser.Type.from(user.type),
    seriesHideFromContinueListening = user.seriesHideFromContinueListening,
    isActive = user.isActive,
    isLocked = user.isLocked,
    lastSeen = user.lastSeen,
    createdAt = user.createdAt,
    permission_download = user.permissions.download,
    permission_update = user.permissions.update,
    permission_delete = user.permissions.delete,
    permission_upload = user.permissions.upload,
    permission_accessAllLibraries = user.permissions.accessAllLibraries,
    permission_accessAllTags = user.permissions.accessAllTags,
    permission_accessExplicitContent = user.permissions.accessExplicitContent,
    librariesAccessible = user.librariesAccessible,
    itemTagsAccessible = user.itemTagsAccessible ?: emptyList(),
  )

  user.mediaProgress.forEach { progress ->
    mediaProgressQueries.insert(progress.asDbModel())
  }

  user.bookmarks.forEach { bookmark ->
    bookmarksQueries.insert(bookmark.asDbModel(user.id))
  }
}
