package app.campfire.core.model

data class Server(
  val url: String,
  val user: User,
  val name: String,
  val tent: Tent,
  val settings: Settings,
) {

  data class Settings(
    val scannerFindCovers: Boolean,
    val scannerCoverProvider: String,
    val scannerParseSubtitle: Boolean,
    val scannerPreferMatchedMetadata: Boolean,
    val scannerDisableWatcher: Boolean,
    val storeCoverWithItem: Boolean,
    val storeMetadataWithItem: Boolean,
    val metadataFileFormat: String,
    val rateLimitLoginRequests: Int,
    val rateLimitLoginWindow: Int,
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
}
