package app.campfire.account.server

import app.campfire.core.model.Server
import app.campfire.data.Server as DbServer

fun DbServer.asDomainModel(): Server {
  return Server(
    url = url,
    name = name,
    tent = tent,
    settings = Server.Settings(
      scannerFindCovers = scannerFindCovers,
      scannerCoverProvider = scannerCoverProvider,
      scannerParseSubtitle = scannerParseSubtitle,
      scannerPreferMatchedMetadata = scannerPreferMatchedMetadata,
      scannerDisableWatcher = scannerDisableWatcher,
      storeCoverWithItem = storeCoverWithItem,
      storeMetadataWithItem = storeMetadataWithItem,
      metadataFileFormat = metadataFileFormat,
      rateLimitLoginRequests = rateLimitLoginRequests,
      rateLimitLoginWindow = rateLimitLoginWindow.toInt(),
      backupSchedule = backupSchedule,
      backupsToKeep = backupsToKeep,
      maxBackupSize = maxBackupSize,
      loggerDailyLogsToKeep = loggerDailyLogsToKeep,
      loggerScannerLogsToKeep = loggerScannerLogsToKeep,
      homeBookshelfView = homeBookshelfView,
      bookshelfView = bookshelfView,
      sortingIgnorePrefix = sortingIgnorePrefix,
      sortingPrefixes = sortingPrefixes,
      chromecastEnabled = chromecastEnabled,
      dateFormat = dateFormat,
      timeFormat = timeFormat,
      language = language,
      logLevel = logLevel,
      version = version,
    ),
  )
}
