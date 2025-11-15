package app.campfire.data.mapping

import app.campfire.core.model.Tent
import app.campfire.data.Server as DatabaseServer
import app.campfire.network.models.ServerSettings

fun ServerSettings.asDatabaseModel(
  url: String,
  userId: String,
  name: String,
  tent: Tent,
): DatabaseServer {
  return DatabaseServer(
    url = url,
    userId = userId,
    name = name,
    tent = tent,
    scannerFindCovers = scannerFindCovers,
    scannerCoverProvider = scannerCoverProvider,
    scannerParseSubtitle = scannerParseSubtitle,
    scannerPreferMatchedMetadata = scannerPreferMatchedMetadata,
    scannerDisableWatcher = scannerDisableWatcher,
    storeCoverWithItem = storeCoverWithItem,
    storeMetadataWithItem = storeMetadataWithItem,
    metadataFileFormat = metadataFileFormat,
    rateLimitLoginRequests = rateLimitLoginRequests,
    rateLimitLoginWindow = rateLimitLoginWindow,
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
  )
}
