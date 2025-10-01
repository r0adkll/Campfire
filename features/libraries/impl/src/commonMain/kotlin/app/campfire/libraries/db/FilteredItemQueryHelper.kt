package app.campfire.libraries.db

import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryId
import app.campfire.core.model.MediaType
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import app.campfire.data.mapping.model.LibraryItemWithMedia
import app.campfire.data.mapping.model.mapToLibraryItem
import app.campfire.db.DatabaseAdapters
import app.campfire.libraries.api.LibraryItemFilter
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import me.tatarka.inject.annotations.Inject

@Inject
class FilteredItemQueryHelper(
  private val sqlDriver: SqlDriver,
  private val adapters: DatabaseAdapters,
) {

  fun count(
    filter: LibraryItemFilter?,
    sortMode: SortMode,
    sortDirection: SortDirection,
    libraryId: LibraryId,
  ): Query<Long> {
    return CountForFilteredItemQuery(
      filter = filter,
      sortMode = sortMode,
      sortDirection = sortDirection,
      libraryId = libraryId,
      mapper = { cursor -> cursor.getLong(0)!! },
    )
  }

  fun select(
    filter: LibraryItemFilter?,
    sortMode: SortMode,
    sortDirection: SortDirection,
    libraryId: LibraryId,
    page: Page? = null,
  ): Query<LibraryItemWithMedia> {
    return SelectFilteredItemQuery(
      filter = filter,
      sortMode = sortMode,
      sortDirection = sortDirection,
      libraryId = libraryId,
      page = page,
      mapper = { cursor ->
        mapCursorToLibraryItem(
          cursor = cursor,
          mapper = ::mapToLibraryItem,
        )
      },
    )
  }

  /**
   * This mapping adapter is super fragile and copied from the
   * generated LibraryItemQueries of SQLDelight. If the schema for these tables
   * are changed at all then this will need to be updated or it will break.
   *
   * TODO: Write unit tests around this
   */
  private fun <T> mapCursorToLibraryItem(
    cursor: SqlCursor,
    mapper: (
      id: String,
      ino: String,
      libraryId: String,
      oldLibraryItemId: String?,
      folderId: String,
      path: String,
      relPath: String,
      isFile: Boolean,
      mtimeMs: Long,
      ctimeMs: Long,
      birthtimeMs: Long,
      addedAt: Long,
      updatedAt: Long,
      isMissing: Boolean,
      isInvalid: Boolean,
      mediaType: MediaType,
      numFiles: Int,
      size: Long,
      serverUrl: String,
      mediaId: String,
      coverPath: String?,
      tags: List<String>?,
      numTracks: Int,
      numAudioFiles: Int,
      numChapters: Int,
      numMissingParts: Int,
      numInvalidAudioFiles: Int,
      durationInMillis: Long,
      sizeInBytes: Long,
      propertySize: Int?,
      ebookFormat: String?,
      metadata_title: String?,
      metadata_subtitle: String?,
      metadata_genres: List<String>?,
      metadata_publishedYear: String?,
      metadata_publishedDate: String?,
      metadata_publisher: String?,
      metadata_description: String?,
      metadata_isbn: String?,
      metadata_asin: String?,
      metadata_language: String?,
      metadata_explicit: Boolean,
      metadata_abridged: Boolean,
      metadata_titleIgnorePrefix: String?,
      metadata_authorName: String?,
      metadata_authorNameLF: String?,
      metadata_narratorName: String?,
      metadata_seriesName: String?,
      metadata_series_id: String?,
      metadata_series_name: String?,
      metadata_series_sequence: Int?,
      libraryItemId: String,
    ) -> T,
  ): T {
    return mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getBoolean(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getBoolean(13)!!,
      cursor.getBoolean(14)!!,
      adapters.libraryItemAdapter.mediaTypeAdapter.decode(cursor.getString(15)!!),
      adapters.libraryItemAdapter.numFilesAdapter.decode(cursor.getLong(16)!!),
      cursor.getLong(17)!!,
      cursor.getString(18)!!,
      cursor.getString(19)!!,
      cursor.getString(20),
      cursor.getString(21)?.let { adapters.mediaAdapter.tagsAdapter.decode(it) },
      adapters.mediaAdapter.numTracksAdapter.decode(cursor.getLong(22)!!),
      adapters.mediaAdapter.numAudioFilesAdapter.decode(cursor.getLong(23)!!),
      adapters.mediaAdapter.numChaptersAdapter.decode(cursor.getLong(24)!!),
      adapters.mediaAdapter.numMissingPartsAdapter.decode(cursor.getLong(25)!!),
      adapters.mediaAdapter.numInvalidAudioFilesAdapter.decode(cursor.getLong(26)!!),
      cursor.getLong(27)!!,
      cursor.getLong(28)!!,
      cursor.getLong(29)?.let { adapters.mediaAdapter.propertySizeAdapter.decode(it) },
      cursor.getString(30),
      cursor.getString(31),
      cursor.getString(32),
      cursor.getString(33)?.let { adapters.mediaAdapter.metadata_genresAdapter.decode(it) },
      cursor.getString(34),
      cursor.getString(35),
      cursor.getString(36),
      cursor.getString(37),
      cursor.getString(38),
      cursor.getString(39),
      cursor.getString(40),
      cursor.getBoolean(41)!!,
      cursor.getBoolean(42)!!,
      cursor.getString(43),
      cursor.getString(44),
      cursor.getString(45),
      cursor.getString(46),
      cursor.getString(47),
      cursor.getString(48),
      cursor.getString(49),
      cursor.getLong(50)?.let { adapters.mediaAdapter.metadata_series_sequenceAdapter.decode(it) },
      cursor.getString(51)!!,
    )
  }

  private inner class SelectFilteredItemQuery<out T : Any>(
    val filter: LibraryItemFilter?,
    val sortMode: SortMode,
    val sortDirection: SortDirection,
    val libraryId: LibraryId,
    val page: Page? = null,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {

    override fun addListener(listener: Listener) {
      sqlDriver.addListener("libraryItem", "media", listener = listener)
    }

    override fun removeListener(listener: Listener) {
      sqlDriver.removeListener("libraryItem", "media", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      val binderBuilder = PreparedSqlStatementBinderBuilder(1)
      val query = binderBuilder.buildQuery(
        """
          |SELECT libraryItem.*,media.* FROM libraryItem
          |INNER JOIN media ON media.libraryItemId = libraryItem.id
          |LEFT JOIN mediaProgress ON mediaProgress.libraryItemId = libraryItem.id
          |WHERE libraryItem.libraryId = ?
        """.trimMargin(),
        filter,
        sortMode,
        sortDirection,
        page,
      )
      bark { "Querying:\n$query" }
      val numParameters = query.count { it == '?' }
      return sqlDriver.executeQuery(
        query.hashCode(),
        query,
        mapper,
        numParameters,
      ) {
        bindString(0, libraryId)
        binderBuilder.apply(this)
      }.also { qr ->
        bark { "SelectFilteredItemQuery Result: \n${qr.value}" }
      }
    }

    override fun toString(): String {
      return "libraryItems.sq:selectFiltered"
    }
  }

  private inner class CountForFilteredItemQuery<out T : Any>(
    val filter: LibraryItemFilter?,
    val sortMode: SortMode,
    val sortDirection: SortDirection,
    val libraryId: LibraryId,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {

    override fun addListener(listener: Listener) {
      sqlDriver.addListener("libraryItem", "media", listener = listener)
    }

    override fun removeListener(listener: Listener) {
      sqlDriver.removeListener("libraryItem", "media", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      val binderBuilder = PreparedSqlStatementBinderBuilder(1)
      val query = binderBuilder.buildQuery(
        """
          |SELECT count(*) FROM libraryItem
          |INNER JOIN media ON media.libraryItemId = libraryItem.id
          |LEFT JOIN mediaProgress ON mediaProgress.libraryItemId = libraryItem.id
          |WHERE libraryItem.libraryId = ?
        """.trimMargin(),
        filter,
        sortMode,
        sortDirection,
      )
      bark { "Querying:\n$query" }
      val numParameters = query.count { it == '?' }
      return sqlDriver.executeQuery(
        query.hashCode(),
        query,
        mapper,
        numParameters
          .also { bark { "CountForFilteredItemQuery: $it" } },
      ) {
        bindString(0, libraryId)
        binderBuilder.apply(this)
      }
    }

    override fun toString(): String {
      return "libraryItems.sq:countFiltered"
    }
  }

  private fun PreparedSqlStatementBinderBuilder.buildQuery(
    initialQuery: String,
    filter: LibraryItemFilter?,
    sortMode: SortMode,
    sortDirection: SortDirection,
    page: Page? = null,
  ): String {
    return buildString {
      appendLine(initialQuery)

      when (filter) {
        is LibraryItemFilter.Authors -> {
          appendLine("AND media.metadata_authorName = ?")
          bind {
            bindString(filter.authorName)
          }
        }

        is LibraryItemFilter.Genres -> {
          appendLine("AND media.metadata_genres = ?")
          bind {
            bindString(filter.value)
          }
        }

        is LibraryItemFilter.Languages -> {
          appendLine("AND media.metadata_language = ?")
          bind {
            bindString(filter.value)
          }
        }

        is LibraryItemFilter.Missing -> when (filter.type) {
          LibraryItemFilter.Missing.Type.ASIN -> {
            appendLine("AND media.metadata_asin IS NULL")
          }

          LibraryItemFilter.Missing.Type.ISBN -> {
            appendLine("AND media.metadata_isbn IS NULL")
          }

          LibraryItemFilter.Missing.Type.SUBTITLE -> {
            appendLine("AND media.metadata_subtitle IS NULL")
          }

          LibraryItemFilter.Missing.Type.AUTHORS -> {
            appendLine("AND media.metadata_authorName IS NULL")
          }

          LibraryItemFilter.Missing.Type.PUBLISHED_YEAR -> {
            appendLine("AND media.metadata_publishedYear IS NULL")
          }

          LibraryItemFilter.Missing.Type.SERIES -> {
            appendLine("AND media.metadata_seriesName IS NULL")
          }

          LibraryItemFilter.Missing.Type.DESCRIPTION -> {
            appendLine("AND media.metadata_description IS NULL")
          }

          LibraryItemFilter.Missing.Type.GENRES -> {
            appendLine("AND media.metadata_genres IS NULL")
          }

          LibraryItemFilter.Missing.Type.TAGS -> {
            appendLine("AND media.tags IS NULL")
          }

          LibraryItemFilter.Missing.Type.NARRATORS -> {
            appendLine("AND media.metadata_narratorName IS NULL")
          }

          LibraryItemFilter.Missing.Type.PUBLISHER -> {
            appendLine("AND media.metadata_publisher IS NULL")
          }

          LibraryItemFilter.Missing.Type.LANGUAGE -> {
            appendLine("AND media.metadata_language IS NULL")
          }
        }

        is LibraryItemFilter.Narrators -> {
          appendLine("AND media.metadata_narratorName = ?")
          bind {
            bindString(filter.value)
          }
        }

        is LibraryItemFilter.Progress -> when (filter.type) {
          LibraryItemFilter.Progress.Type.Finished -> {
            appendLine("AND mediaProgress.isFinished = 1")
          }

          LibraryItemFilter.Progress.Type.NotFinished -> {
            appendLine("AND mediaProgress.isFinished = 0")
          }

          LibraryItemFilter.Progress.Type.NotStarted -> {
            appendLine("AND mediaProgress.id IS NULL")
          }

          LibraryItemFilter.Progress.Type.InProgress -> {
            appendLine("AND mediaProgress.id IS NOT NULL AND mediaProgress.isFinished = 0")
          }
        }

        is LibraryItemFilter.Series -> {
          appendLine("AND media.metadata_seriesName = ?")
          bind {
            bindString(filter.value)
          }
        }

        is LibraryItemFilter.Tags -> {
          appendLine("AND media.tags LIKE ?")
          bind {
            bindString("%${filter.value}%")
          }
        }

        is LibraryItemFilter.Tracks -> when (filter.type) {
          LibraryItemFilter.Tracks.Type.Single -> {
            appendLine("AND media.numTracks = 1")
          }

          LibraryItemFilter.Tracks.Type.Multi -> {
            appendLine("AND media.numTracks > 1")
          }
        }

        null -> Unit
      }

      append("ORDER BY ")
      when (sortMode) {
        SortMode.Title -> append("media.metadata_title")
        SortMode.AuthorFL -> append("media.metadata_authorName")
        SortMode.AuthorLF -> append("media.metadata_authorNameLF")
        SortMode.PublishYear -> append("media.metadata_publishedYear")
        SortMode.AddedAt -> append("libraryItem.addedAt")
        SortMode.Size -> append("libraryItem.size")
        SortMode.Duration -> append("media.durationInMillis")
      }
      append(" ")
      appendLine(
        when (sortDirection) {
          SortDirection.Ascending -> "ASC"
          SortDirection.Descending -> "DESC"
        },
      )

      page?.let {
        appendLine("LIMIT ? OFFSET ?")
        bind {
          bindLong(it.limit.toLong())
          bindLong(it.offset.toLong())
        }
      }
    }
  }
}

class Page(
  val limit: Int,
  val offset: Int,
)

class PreparedSqlStatementBinderBuilder(initialIndex: Int = 0) {
  private val binders = mutableListOf<SqlPreparedStatement.() -> Unit>()
  private var currentIndex = initialIndex

  fun bind(binder: AutoIncrementingSqlPreparedStatement.() -> Unit) {
    binders.add(
      {
        AutoIncrementingSqlPreparedStatement(this) {
          currentIndex++
        }.binder()
      },
    )
  }

  fun apply(statement: SqlPreparedStatement) {
    binders.forEach { it(statement) }
  }
}
