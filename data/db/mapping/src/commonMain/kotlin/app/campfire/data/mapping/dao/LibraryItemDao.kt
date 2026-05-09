package app.campfire.data.mapping.dao

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import app.campfire.core.model.loggableId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.data.LibraryItem as DbLibraryItem
import app.campfire.data.MediaAudioFiles
import app.campfire.data.MediaAudioTracks
import app.campfire.data.MediaChapters
import app.campfire.data.MetadataAuthor
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asEpisodeDbModel
import app.campfire.data.mapping.model.LibraryItemWithMedia
import app.campfire.data.mapping.model.PodcastLibraryItemWithMedia
import app.campfire.data.mapping.model.mapToLibraryItemWithProgress
import app.campfire.data.mapping.model.mapToPodcastLibraryItemWithProgress
import app.campfire.network.models.LibraryItemExpanded
import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * This is an experimental pattern for access db or composite-db operations in a
 * reusable manner. Subject to change.
 */
interface LibraryItemDao {

  /**
   * Fully hydrate the common [LibraryItemWithMedia] model from the database, pull all its
   * relational data to compose the full [LibraryItem] domain object
   * @param item the common database model for a library item with its associated media/metadata
   * @return a fully hydrated [LibraryItem] domain model
   */
  suspend fun hydrateItem(item: LibraryItemWithMedia): LibraryItem

  /**
   * Hydrate a podcast library item — composes [Media.Podcast] from the joined wrapper plus
   * the episodes pulled separately from the [podcastEpisode] table.
   */
  suspend fun hydratePodcastItem(item: PodcastLibraryItemWithMedia): LibraryItem

  /**
   * Hydrate a paged library item from its bare [libraryItem] row. Dispatches on
   * [DbLibraryItem.mediaType] to read the matching media variant (book or podcast)
   * with progress, then runs the corresponding hydrate path. Used by the paging
   * source so a single page query can mix book and podcast items.
   */
  suspend fun hydratePagedItem(item: DbLibraryItem): LibraryItem

  /**
   * Hydrate by library item id alone — looks up the media type and dispatches to the
   * matching hydrate path. Returns null if no item exists for the id. Used where we
   * carry only an id (e.g. queue rows, server-resolved sessions).
   */
  suspend fun hydrateById(id: app.campfire.core.model.LibraryItemId): LibraryItem?

  /**
   * Insert an expanded library item and all of its relations in a transaction
   * @param item the [LibraryItemExpanded] network model to insert into the database
   * @param asTransaction true to wrap database insertion in a transaction, false to just execute
   */
  suspend fun insert(
    item: LibraryItemExpanded,
    asTransaction: Boolean = true,
    ignoreOnInsert: Boolean = false,
  )

  /**
   * Insert an expanded library item and all of its relations in a transaction
   * @param item the [LibraryItemExpanded] network model to insert into the database
   * @param asTransaction true to wrap database insertion in a transaction, false to just execute
   */
  suspend fun insert(
    item: LibraryItem,
    asTransaction: Boolean = true,
    ignoreOnInsert: Boolean = false,
  )
}

@ContributesBinding(UserScope::class)
@Inject
class SqlDelightLibraryItemDao(
  private val db: CampfireDatabase,
  private val urlHydrator: UrlHydrator,
  private val dispatcherProvider: DispatcherProvider,
  private val userSession: UserSession,
) : LibraryItemDao {

  override suspend fun hydrateItem(
    item: LibraryItemWithMedia,
  ): LibraryItem = withContext(dispatcherProvider.databaseRead) {
    val (audioFiles, audioTracks, chapters, authors) = db.transactionWithResult {
      val audioFiles = db.mediaAudioFilesQueries
        .selectForMediaId(item.mediaId)
        .awaitAsList()

      val audioTracks = db.mediaAudioTracksQueries
        .selectForMediaId(item.mediaId)
        .awaitAsList()

      val chapters = db.mediaChaptersQueries
        .selectForMediaId(item.mediaId)
        .awaitAsList()

      val authors = db.metadataAuthorQueries
        .selectForMediaId(item.mediaId)
        .awaitAsList()

      LibraryItemRelationalData(audioFiles, audioTracks, chapters, authors)
    }

    item.asDomainModel(
      urlHydrator,
      audioFiles,
      audioTracks,
      chapters,
      authors,
    )
  }

  override suspend fun hydratePodcastItem(
    item: PodcastLibraryItemWithMedia,
  ): LibraryItem = withContext(dispatcherProvider.databaseRead) {
    val episodes = db.podcastEpisodeQueries
      .selectForLibraryItemId(item.id)
      .awaitAsList()
    val audioTracksByEpisodeId = db.podcastEpisodeAudioTrackQueries
      .selectForLibraryItemId(item.id)
      .awaitAsList()
      .associateBy(
        keySelector = { it.episodeId },
        valueTransform = { it.asDomainModel(urlHydrator) },
      )

    item.asDomainModel(urlHydrator, episodes, audioTracksByEpisodeId)
  }

  override suspend fun hydratePagedItem(
    item: DbLibraryItem,
  ): LibraryItem = when (item.mediaType) {
    MediaType.Book -> {
      val full = withContext(dispatcherProvider.databaseRead) {
        db.libraryItemsQueries
          .selectForIdFull(item.id, ::mapToLibraryItemWithProgress)
          .awaitAsOne()
      }
      hydrateItem(full)
    }
    MediaType.Podcast -> {
      val full = withContext(dispatcherProvider.databaseRead) {
        db.libraryItemsQueries
          .selectForPodcastIdFull(item.id, ::mapToPodcastLibraryItemWithProgress)
          .awaitAsOne()
      }
      hydratePodcastItem(full)
    }
  }

  override suspend fun hydrateById(
    id: app.campfire.core.model.LibraryItemId,
  ): LibraryItem? {
    val mediaType = withContext(dispatcherProvider.databaseRead) {
      db.libraryItemsQueries.selectMediaTypeForId(id).awaitAsOneOrNull()
    } ?: return null
    return when (mediaType) {
      MediaType.Book -> {
        val full = withContext(dispatcherProvider.databaseRead) {
          db.libraryItemsQueries
            .selectForIdFull(id, ::mapToLibraryItemWithProgress)
            .awaitAsOneOrNull()
        } ?: return null
        hydrateItem(full)
      }
      MediaType.Podcast -> {
        val full = withContext(dispatcherProvider.databaseRead) {
          db.libraryItemsQueries
            .selectForPodcastIdFull(id, ::mapToPodcastLibraryItemWithProgress)
            .awaitAsOneOrNull()
        } ?: return null
        hydratePodcastItem(full)
      }
    }
  }

  private data class LibraryItemRelationalData(
    val audioFiles: List<MediaAudioFiles>,
    val audioTracks: List<MediaAudioTracks>,
    val chapters: List<MediaChapters>,
    val metadataAuthors: List<MetadataAuthor>,
  )

  override suspend fun insert(
    item: LibraryItemExpanded,
    asTransaction: Boolean,
    ignoreOnInsert: Boolean,
  ) = when (item) {
    is LibraryItemExpanded.Book -> insertBook(item, asTransaction, ignoreOnInsert)
    is LibraryItemExpanded.Podcast -> insertPodcast(item, asTransaction, ignoreOnInsert)
  }

  private suspend fun insertPodcast(
    item: LibraryItemExpanded.Podcast,
    asTransaction: Boolean,
    ignoreOnInsert: Boolean,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transactionIf(asTransaction) {
      val libraryItem = item.asDbModel()

      // 1) Root library item row
      if (ignoreOnInsert) {
        db.libraryItemsQueries.insertOrIgnore(libraryItem)
      } else {
        db.libraryItemsQueries.insert(libraryItem)
      }

      // 2) Podcast media row (mirrors the book write — different table)
      val podcastMedia = item.media.asDbModel(libraryItem.id, urlHydrator)
      if (ignoreOnInsert) {
        db.podcastMediaQueries.insertOrIgnore(podcastMedia)
      } else {
        db.podcastMediaQueries.insert(podcastMedia)
      }

      // 3) Episode rows + the per-episode audio track that backs playback. The track is
      // only present on the expanded JSON shape; on the basic shape we leave the track
      // table empty for that episode and let a later expanded fetch fill it in.
      item.media.episodes?.forEach { episode ->
        val row = episode.asDbModel(
          libraryItemId = libraryItem.id,
          podcastMediaId = podcastMedia.mediaId,
        )
        if (ignoreOnInsert) {
          db.podcastEpisodeQueries.insertOrIgnore(row)
        } else {
          db.podcastEpisodeQueries.insert(row)
        }
        episode.audioTrack?.let { track ->
          db.podcastEpisodeAudioTrackQueries.insert(
            track.asEpisodeDbModel(episodeId = episode.id),
          )
        }
      }

      // 4) Per-episode (or item-level) user progress. The server returns a single
      // MediaProgress here keyed by `episodeId` for podcasts — usually the most recently
      // played episode — so the freshness check must compare against the row at the
      // *same* (libraryItemId, episodeId) key the insert is about to replace, not the
      // book/item-level row at episodeId = ''.
      item.userMediaProgress?.let { progress ->
        val existing = db.mediaProgressQueries.selectForEpisode(
          userId = progress.userId,
          libraryItemId = libraryItem.id,
          episodeId = progress.episodeId.orEmpty(),
        ).executeAsOneOrNull()
        if (existing == null || existing.lastUpdate <= progress.lastUpdate) {
          db.mediaProgressQueries.insert(progress.asDbModel())
        }
      }

      afterCommit {
        bark("LibraryItemDao", LogPriority.VERBOSE) {
          "LibraryItemExpandedPodcast[${item.id.loggableId}] inserted"
        }
      }

      afterRollback {
        bark("LibraryItemDao", LogPriority.VERBOSE) {
          "LibraryItemExpandedPodcast[${item.id.loggableId}] insert failed, rolling back"
        }
      }
    }
  }

  private suspend fun insertBook(
    item: LibraryItemExpanded.Book,
    asTransaction: Boolean,
    ignoreOnInsert: Boolean,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transactionIf(asTransaction) {
      val libraryItem = item.asDbModel()

      // 1) Insert the root library item
      if (ignoreOnInsert) {
        db.libraryItemsQueries.insertOrIgnore(libraryItem)
      } else {
        db.libraryItemsQueries.insert(libraryItem)
      }

      // 2) Insert the media meta
      val media = item.media.asDbModel(libraryItem.id)
      if (ignoreOnInsert) {
        db.mediaQueries.insertOrIgnore(media)
      } else {
        db.mediaQueries.insert(media)
      }

      // 3) Insert relations

      item.userMediaProgress?.let { progress ->
        // Match the insert's PK so the freshness check compares against the row that's
        // about to be replaced, not the empty-episodeId book row.
        val existing = db.mediaProgressQueries.selectForEpisode(
          userId = progress.userId,
          libraryItemId = libraryItem.id,
          episodeId = progress.episodeId.orEmpty(),
        ).executeAsOneOrNull()
        if (existing == null || existing.lastUpdate <= progress.lastUpdate) {
          db.mediaProgressQueries.insert(progress.asDbModel())
        }
      }

      item.media.audioFiles.forEach { audioFile ->
        db.mediaAudioFilesQueries.insert(audioFile.asDbModel(media.mediaId))
      }

      item.media.chapters.forEach { chapter ->
        db.mediaChaptersQueries.insert(chapter.asDbModel(media.mediaId))
      }

      item.media.tracks.forEach { track ->
        db.mediaAudioTracksQueries.insert(track.asDbModel(media.mediaId))
      }

      item.media.metadata.authors?.forEach { authorMeta ->
        db.metadataAuthorQueries.insert(authorMeta.asDbModel(media.mediaId))
      }

      afterCommit {
        bark("LibraryItemDao", LogPriority.VERBOSE) {
          "LibraryItemExpanded[${item.id.loggableId}] inserted"
        }
      }

      afterRollback {
        bark("LibraryItemDao", LogPriority.VERBOSE) {
          "LibraryItemExpanded[${item.id.loggableId}] insert failed, rolling back"
        }
      }
    }
  }

  override suspend fun insert(
    item: LibraryItem,
    asTransaction: Boolean,
    ignoreOnInsert: Boolean,
  ) = when (item.media) {
    is Media.Book -> insertBookDomain(item, asTransaction, ignoreOnInsert)
    is Media.Podcast -> insertPodcastDomain(item, asTransaction, ignoreOnInsert)
  }

  private suspend fun insertBookDomain(
    item: LibraryItem,
    asTransaction: Boolean,
    ignoreOnInsert: Boolean,
  ) = withContext(dispatcherProvider.databaseWrite) {
    val book = item.media as Media.Book
    db.transactionIf(asTransaction) {
      val libraryItem = item.asDbModel(userSession.serverUrl!!)

      if (ignoreOnInsert) {
        db.libraryItemsQueries.insertOrIgnore(libraryItem)
      } else {
        db.libraryItemsQueries.insert(libraryItem)
      }

      val media = item.media.asDbModel(libraryItem.id)
      if (ignoreOnInsert) {
        db.mediaQueries.insertOrIgnore(media)
      } else {
        db.mediaQueries.insert(media)
      }

      item.userMediaProgress?.let { progress ->
        val existing = db.mediaProgressQueries.selectForEpisode(
          userId = progress.userId,
          libraryItemId = libraryItem.id,
          episodeId = progress.episodeId.orEmpty(),
        ).executeAsOneOrNull()
        if (existing == null || existing.lastUpdate <= progress.lastUpdate) {
          db.mediaProgressQueries.insert(progress.asDbModel())
        }
      }

      book.audioFiles.forEach { audioFile ->
        db.mediaAudioFilesQueries.insert(audioFile.asDbModel(media.mediaId))
      }

      book.chapters.forEach { chapter ->
        db.mediaChaptersQueries.insert(chapter.asDbModel(media.mediaId))
      }

      book.tracks.forEach { track ->
        db.mediaAudioTracksQueries.insert(track.asDbModel(media.mediaId))
      }

      book.metadata.authors.forEach { authorMeta ->
        db.metadataAuthorQueries.insert(authorMeta.asDbModel(media.mediaId))
      }

      afterCommit {
        bark("LibraryItemDao", LogPriority.VERBOSE) {
          "LibraryItem[${item.id.loggableId}] (book) inserted"
        }
      }
      afterRollback {
        bark("LibraryItemDao", LogPriority.VERBOSE) {
          "LibraryItem[${item.id.loggableId}] (book) insert failed, rolling back"
        }
      }
    }
  }

  private suspend fun insertPodcastDomain(
    item: LibraryItem,
    asTransaction: Boolean,
    ignoreOnInsert: Boolean,
  ) = withContext(dispatcherProvider.databaseWrite) {
    val podcast = item.media as Media.Podcast
    db.transactionIf(asTransaction) {
      val libraryItem = item.asDbModel(userSession.serverUrl!!)

      if (ignoreOnInsert) {
        db.libraryItemsQueries.insertOrIgnore(libraryItem)
      } else {
        db.libraryItemsQueries.insert(libraryItem)
      }

      val podcastMedia = podcast.asDbModel(libraryItem.id)
      if (ignoreOnInsert) {
        db.podcastMediaQueries.insertOrIgnore(podcastMedia)
      } else {
        db.podcastMediaQueries.insert(podcastMedia)
      }

      podcast.episodes.forEach { episode ->
        val row = episode.asDbModel(podcastMediaId = podcastMedia.mediaId)
        if (ignoreOnInsert) {
          db.podcastEpisodeQueries.insertOrIgnore(row)
        } else {
          db.podcastEpisodeQueries.insert(row)
        }
        episode.audioTrack?.let { track ->
          db.podcastEpisodeAudioTrackQueries.insert(
            track.asEpisodeDbModel(episodeId = episode.id),
          )
        }
      }

      item.userMediaProgress?.let { progress ->
        val existing = db.mediaProgressQueries.selectForEpisode(
          userId = progress.userId,
          libraryItemId = libraryItem.id,
          episodeId = progress.episodeId.orEmpty(),
        ).executeAsOneOrNull()
        if (existing == null || existing.lastUpdate <= progress.lastUpdate) {
          db.mediaProgressQueries.insert(progress.asDbModel())
        }
      }

      afterCommit {
        bark("LibraryItemDao", LogPriority.VERBOSE) {
          "LibraryItem[${item.id.loggableId}] (podcast) inserted"
        }
      }
      afterRollback {
        bark("LibraryItemDao", LogPriority.VERBOSE) {
          "LibraryItem[${item.id.loggableId}] (podcast) insert failed, rolling back"
        }
      }
    }
  }
}

private suspend fun SuspendingTransacter.transactionIf(
  enabled: Boolean,
  body: suspend SuspendingTransactionWithoutReturn.() -> Unit,
) {
  if (enabled) {
    transaction {
      body()
    }
  } else {
    NoOpSuspendingTransactionWithoutReturn().body()
  }
}

private class NoOpSuspendingTransactionWithoutReturn : SuspendingTransactionWithoutReturn {
  override fun afterCommit(function: () -> Unit) = Unit
  override fun afterRollback(function: () -> Unit) = Unit

  override fun rollback(): Nothing {
    error("Operation not supported in non-transaction mode")
  }

  override suspend fun transactionWithResult(body: suspend SuspendingTransactionWithoutReturn.() -> Unit) {
    error("Operation not supported in non-transaction mode")
  }
}
