package app.campfire.data.mapping.dao

import app.campfire.CampfireDatabase
import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.data.MediaAudioFiles
import app.campfire.data.MediaAudioTracks
import app.campfire.data.MediaChapters
import app.campfire.data.MetadataAuthor
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.model.LibraryItemWithMedia
import app.campfire.network.models.LibraryItemExpanded
import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.async.coroutines.awaitAsList
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
   * Insert an expanded library item and all of its relations in a transaction
   * @param item the [LibraryItemExpanded] network model to insert into the database
   * @param asTransaction true to wrap database insertion in a transaction, false to just execute
   */
  suspend fun insert(
    item: LibraryItemExpanded,
    asTransaction: Boolean = true,
    ignoreOnInsert: Boolean = false,
  )
}

@ContributesBinding(UserScope::class)
@Inject
class SqlDelightLibraryItemDao(
  private val db: CampfireDatabase,
  private val coverImageHydrator: CoverImageHydrator,
  private val dispatcherProvider: DispatcherProvider,
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
      coverImageHydrator,
      audioFiles,
      audioTracks,
      chapters,
      authors,
    )
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
        // Only insert the media progress if the one we have locally isn't newer
        val existing = db.mediaProgressQueries.selectForLibraryItem(
          userId = progress.userId,
          libraryItemId = libraryItem.id,
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
          "LibraryItemExpanded[${item.id}] inserted"
        }
      }

      afterRollback {
        bark("LibraryItemDao", LogPriority.VERBOSE) {
          "LibraryItemExpanded[${item.id}] insert failed, rolling back"
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
