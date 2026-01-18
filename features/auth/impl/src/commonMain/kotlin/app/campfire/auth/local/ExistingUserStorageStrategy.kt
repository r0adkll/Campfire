package app.campfire.auth.local

import app.campfire.CampfireDatabase
import app.campfire.auth.di.ExistingUser
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.model.Tent
import app.campfire.data.mapping.asDbModel
import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@ExistingUser
@ContributesBinding(AppScope::class)
@Inject
class ExistingUserStorageStrategy(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : UserStorageStrategy {

  override suspend fun store(
    tent: Tent,
    serverName: String,
    serverUrl: String,
    serverSettings: ServerSettings,
    user: User,
    userDefaultLibraryId: String,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      // Update Server/Settings
      db.serversQueries.update(
        name = serverName,
        tent = tent,
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

      // Update User
      db.usersQueries.update(
        id = user.id,
        name = user.username,
        type = app.campfire.core.model.User.Type.from(user.type),
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

      // Insert User MediaProgress
      user.mediaProgress.forEach { progress ->
        db.mediaProgressQueries.insert(progress.asDbModel())
      }

      // Insert User Bookmarks
      user.bookmarks.forEach { bookmark ->
        db.bookmarksQueries.insert(bookmark.asDbModel(user.id))
      }
    }
  }
}
