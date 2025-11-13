package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerSettings(
  val id: String,
  val scannerFindCovers: Boolean,
  val scannerCoverProvider: String,
  val scannerParseSubtitle: Boolean,
  val scannerPreferMatchedMetadata: Boolean,
  val scannerDisableWatcher: Boolean,
  val storeCoverWithItem: Boolean,
  val storeMetadataWithItem: Boolean,
  val metadataFileFormat: String,
  val rateLimitLoginRequests: Int,
  val rateLimitLoginWindow: Long,
  val backupSchedule: String,
  val backupsToKeep: Int,
  val maxBackupSize: Int,
  val loggerDailyLogsToKeep: Int,
  val loggerScannerLogsToKeep: Int,
  val homeBookshelfView: Int,
  val bookshelfView: Int,
  val sortingIgnorePrefix: Boolean,
  val sortingPrefixes: List<String>,
  val chromecastEnabled: Boolean,
  val dateFormat: String,
  val timeFormat: String,
  val language: String,
  val logLevel: Int,
  val version: String,
)
