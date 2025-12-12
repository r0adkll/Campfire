package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.campfire.data.Authors
import app.campfire.data.BookmarkFailedCreate
import app.campfire.data.BookmarkFailedDelete
import app.campfire.data.Bookmarks
import app.campfire.data.FilterData
import app.campfire.data.Library
import app.campfire.data.LibraryItem
import app.campfire.data.Media
import app.campfire.data.MediaAudioFiles
import app.campfire.data.MediaAudioTracks
import app.campfire.data.MediaChapters
import app.campfire.data.MediaProgress
import app.campfire.data.Search_genres
import app.campfire.data.Search_narrators
import app.campfire.data.Search_tags
import app.campfire.data.Server
import app.campfire.data.Session
import app.campfire.data.Shelf
import app.campfire.data.User
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

/**
 * This is used to migrate the database.
 * Checkout [CampfireDatabaseImpl.Schema.version] for the current version. This should match
 * the latest migration file (if > 1) in `sqldelight/migrations`.
 */
private const val OLD_DB_VERSION = 2

@ContributesBinding(AppScope::class)
@Inject
class DatabaseFactory(
  private val driver: SqlDriver,
) : DatabaseAdapters {

  override val libraryItemAdapter: LibraryItem.Adapter
    get() = LibraryItem.Adapter(
      mediaTypeAdapter = EnumColumnAdapter(),
      numFilesAdapter = IntColumnAdapter,
    )

  override val mediaAdapter: Media.Adapter
    get() = Media.Adapter(
      tagsAdapter = StringListAdapter,
      numTracksAdapter = IntColumnAdapter,
      numChaptersAdapter = IntColumnAdapter,
      numAudioFilesAdapter = IntColumnAdapter,
      numMissingPartsAdapter = IntColumnAdapter,
      numInvalidAudioFilesAdapter = IntColumnAdapter,
      propertySizeAdapter = IntColumnAdapter,
      metadata_genresAdapter = StringListAdapter,
      metadata_series_sequenceAdapter = IntColumnAdapter,
    )

  fun migrate() {
    CampfireDatabase.Schema.migrate(
      driver = driver,
      oldVersion = OLD_DB_VERSION.toLong(),
      newVersion = CampfireDatabase.Schema.version,
    )
  }

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
    libraryItemAdapter = libraryItemAdapter,
    mediaAdapter = mediaAdapter,
    authorsAdapter = Authors.Adapter(
      numBooksAdapter = IntColumnAdapter,
    ),
    mediaAudioFilesAdapter = MediaAudioFiles.Adapter(
      mediaIndexAdapter = IntColumnAdapter,
      bitRateAdapter = IntColumnAdapter,
      channelsAdapter = IntColumnAdapter,
      discNumFromMetaAdapter = IntColumnAdapter,
      trackNumFromMetaAdapter = IntColumnAdapter,
      discNumFromFilenameAdapter = IntColumnAdapter,
      trackNumFromFilenameAdapter = IntColumnAdapter,
    ),
    mediaChaptersAdapter = MediaChapters.Adapter(IntColumnAdapter),
    mediaAudioTracksAdapter = MediaAudioTracks.Adapter(
      mediaIndexAdapter = IntColumnAdapter,
    ),
    mediaProgressAdapter = MediaProgress.Adapter(
      mediaItemTypeAdapter = EnumColumnAdapter(),
    ),
    sessionAdapter = Session.Adapter(
      idAdapter = UuidAdapter,
      playMethodAdapter = EnumColumnAdapter(),
      timeListeningAdapter = DurationAdapter,
      startTimeAdapter = DurationAdapter,
      currentTimeAdapter = DurationAdapter,
      startedAtAdapter = LocalDateTimeAdapter,
      updatedAtAdapter = LocalDateTimeAdapter,
    ),
    bookmarksAdapter = Bookmarks.Adapter(
      timeInSecondsAdapter = IntColumnAdapter,
      createdAtAdapter = LocalDateTimeAdapter,
    ),
    bookmarkFailedCreateAdapter = BookmarkFailedCreate.Adapter(
      timeInSecondsAdapter = IntColumnAdapter,
    ),
    bookmarkFailedDeleteAdapter = BookmarkFailedDelete.Adapter(
      timeInSecondsAdapter = IntColumnAdapter,
    ),
    search_tagsAdapter = Search_tags.Adapter(
      tagsAdapter = BasicSearchResultListAdapter,
    ),
    search_narratorsAdapter = Search_narrators.Adapter(
      narratorsAdapter = BasicSearchResultListAdapter,
    ),
    search_genresAdapter = Search_genres.Adapter(
      genresAdapter = BasicSearchResultListAdapter,
    ),
    filterDataAdapter = FilterData.Adapter(
      bookCountAdapter = IntColumnAdapter,
      authorCountAdapter = IntColumnAdapter,
      seriesCountAdapter = IntColumnAdapter,
      podcastCountAdapter = IntColumnAdapter,
      numIssuesAdapter = IntColumnAdapter,
    ),
    shelfAdapter = Shelf.Adapter(
      totalAdapter = IntColumnAdapter,
      typeAdapter = EnumColumnAdapter(),
      homeOrderAdapter = IntColumnAdapter,
    ),
  )
}
