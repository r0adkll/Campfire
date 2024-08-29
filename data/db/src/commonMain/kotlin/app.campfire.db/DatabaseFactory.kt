package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.data.Library
import app.campfire.data.LibraryItem
import app.campfire.data.Media
import app.campfire.data.Server
import app.campfire.data.User
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import me.tatarka.inject.annotations.Inject

@Inject
class DatabaseFactory(
  private val driver: SqlDriver,
) {
  fun build(): CampfireDatabase = CampfireDatabase(
    driver = driver,
    serverAdapter = Server.Adapter(
      tentAdapter = EnumColumnAdapter(),
      loggerScannerLogsToKeepAdapter = IntColumnAdapter,
      backupsToKeepAdapter = IntColumnAdapter,
      bookshelfViewAdapter = IntColumnAdapter,
      logLevelAdapter = IntColumnAdapter,
      sortingPrefixesAdapter = StringListAdapter,
      homeBookshelfViewAdapter = IntColumnAdapter,
      loggerDailyLogsToKeepAdapter = IntColumnAdapter,
      rateLimitLoginRequestsAdapter = IntColumnAdapter,
      maxBackupSizeAdapter = IntColumnAdapter,
    ),
    userAdapter = User.Adapter(
      typeAdapter = EnumColumnAdapter(),
      itemTagsAccessibleAdapter = StringListAdapter,
      librariesAccessibleAdapter = StringListAdapter,
      seriesHideFromContinueListeningAdapter = StringListAdapter,
    ),
    libraryAdapter = Library.Adapter(
      displayOrderAdapter = IntColumnAdapter,
      coverAspectRatioAdapter = IntColumnAdapter,
    ),
    libraryItemAdapter = LibraryItem.Adapter(
      mediaTypeAdapter = EnumColumnAdapter(),
      numFilesAdapter = IntColumnAdapter,
    ),
    mediaAdapter = Media.Adapter(
      tagsAdapter = StringListAdapter,
      numTracksAdapter = IntColumnAdapter,
      numChaptersAdapter = IntColumnAdapter,
      numAudioFilesAdapter = IntColumnAdapter,
      numMissingPartsAdapter = IntColumnAdapter,
      numInvalidAudioFilesAdapter = IntColumnAdapter,
      propertySizeAdapter = IntColumnAdapter,
      metadata_genresAdapter = StringListAdapter,
      metadata_series_sequenceAdapter = IntColumnAdapter,
    ),
  )
}
