package app.campfire.libraries.items

import app.campfire.CampfireDatabase
import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.libraries.LibraryItemDbData
import app.campfire.network.models.LibraryItemExpanded as NetworkLibraryItem
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

class LibraryItemSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val coverImageHydrator: CoverImageHydrator,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): SourceOfTruth<LibraryItemId, NetworkLibraryItem, LibraryItem> {
    return SourceOfTruth.of(
      reader = { libraryItemId -> readLibraryItem(libraryItemId) },
      writer = { libraryItemId, item -> writeItem(libraryItemId, item) },
      delete = { libraryItemId -> deleteItem(libraryItemId) },
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun readLibraryItem(libraryItemId: LibraryItemId): Flow<LibraryItem?> {
    return db.libraryItemsQueries
      .selectForId(libraryItemId)
      .asFlow()
      .mapToOneOrNull(dispatcherProvider.databaseRead)
      .mapLatest { item ->
        if (item == null) return@mapLatest null
        withContext(dispatcherProvider.databaseRead) {
          val (audioFiles, audioTracks, chapters, authors) = db.transactionWithResult {
            val audioFiles = db.mediaAudioFilesQueries
              .selectForMediaId(item.mediaId)
              .executeAsList()

            val audioTracks = db.mediaAudioTracksQueries
              .selectForMediaId(item.mediaId)
              .executeAsList()

            val chapters = db.mediaChaptersQueries
              .selectForMediaId(item.mediaId)
              .executeAsList()

            val authors = db.metadataAuthorQueries
              .selectForMediaId(item.mediaId)
              .executeAsList()

            LibraryItemDbData(audioFiles, audioTracks, chapters, authors)
          }

          item.asDomainModel(
            coverImageHydrator,
            audioFiles,
            audioTracks,
            chapters,
            authors,
          )
        }
      }
  }

  private suspend fun writeItem(libraryItemId: LibraryItemId, item: NetworkLibraryItem) {
    withContext(dispatcherProvider.databaseWrite) {
      db.transaction {
        val libraryItem = item.asDbModel()

        // 1) Insert the root library item
        db.libraryItemsQueries.insert(libraryItem)

        // 2) Insert the media meta
        val media = item.media.asDbModel(libraryItemId)
        db.mediaQueries.insert(media)

        // 3) Insert relations

        item.userMediaProgress?.let { progress ->
          // Only insert the media progress if the one we have locally isn't newer
          val existing = db.mediaProgressQueries.selectForLibraryItem(
            userId = progress.userId,
            libraryItemId = libraryItemId,
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
      }
    }
  }

  private suspend fun deleteItem(libraryItemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.libraryItemsQueries.deleteForId(libraryItemId)
    }
  }
}
