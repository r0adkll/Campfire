package app.campfire.libraries

import app.campfire.CampfireDatabase
import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.data.MediaAudioFiles
import app.campfire.data.MediaAudioTracks
import app.campfire.data.MediaChapters
import app.campfire.data.MediaProgress
import app.campfire.data.MetadataAuthor
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.LibraryItemExpanded
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.impl.extensions.fresh

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreLibraryItemRepository(
  private val userSession: UserSession,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val coverImageHydrator: CoverImageHydrator,
  private val dispatcherProvider: DispatcherProvider,
) : LibraryItemRepository {

  // TODO: Break store builders into separate components/factory
  //  assignees: r0adkll
  @OptIn(ExperimentalCoroutinesApi::class)
  private val itemStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { itemId: LibraryItemId -> api.getLibraryItem(itemId).asFetcherResult() },
    sourceOfTruth = SourceOfTruth.of(
      reader = { itemId: LibraryItemId ->
        db.libraryItemsQueries
          .selectForId(itemId)
          .asFlow()
          .mapToOneOrNull(dispatcherProvider.databaseRead)
          .mapLatest { item ->
            if (item == null) return@mapLatest null
            withContext(dispatcherProvider.databaseRead) {
              val (audioFiles, audioTracks, chapters, progress, authors) = db.transactionWithResult {
                val audioFiles = db.mediaAudioFilesQueries
                  .selectForMediaId(item.mediaId)
                  .executeAsList()

                val audioTracks = db.mediaAudioTracksQueries
                  .selectForMediaId(item.mediaId)
                  .executeAsList()

                val chapters = db.mediaChaptersQueries
                  .selectForMediaId(item.mediaId)
                  .executeAsList()

                val progress = db.mediaProgressQueries
                  .selectForLibraryItem(item.id)
                  .executeAsOneOrNull()

                val authors = db.metadataAuthorQueries
                  .selectForMediaId(item.mediaId)
                  .executeAsList()

                LibraryItemDbData(audioFiles, audioTracks, chapters, progress, authors)
              }

              item.asDomainModel(
                coverImageHydrator,
                audioFiles,
                audioTracks,
                chapters,
                progress,
                authors,
              )
            }
          }
      },
      writer = { itemId, itemExpanded: LibraryItemExpanded ->
        withContext(dispatcherProvider.databaseWrite) {
          db.transaction {
            val libraryItem = itemExpanded.asDbModel(userSession.serverUrl!!)

            // 1) Insert the root library item
            db.libraryItemsQueries.insert(libraryItem)

            // 2) Insert the media meta
            val media = itemExpanded.media.asDbModel(itemId)
            db.mediaQueries.insert(media)

            // 3) Insert relations

            itemExpanded.userMediaProgress?.let { progress ->
              // Only insert the media progress if the one we have locally isn't newer
              val existing = db.mediaProgressQueries.selectForLibraryItem(itemId).executeAsOneOrNull()
              if (existing == null || existing.lastUpdate < progress.lastUpdate) {
                db.mediaProgressQueries.insert(progress.asDbModel())
              }
            }

            itemExpanded.media.audioFiles.forEach { audioFile ->
              db.mediaAudioFilesQueries.insert(audioFile.asDbModel(media.mediaId))
            }

            itemExpanded.media.chapters.forEach { chapter ->
              db.mediaChaptersQueries.insert(chapter.asDbModel(media.mediaId))
            }

            itemExpanded.media.tracks.forEach { track ->
              db.mediaAudioTracksQueries.insert(track.asDbModel(media.mediaId))
            }

            itemExpanded.media.metadata.authors?.forEach { authorMeta ->
              db.metadataAuthorQueries.insert(authorMeta.asDbModel(media.mediaId))
            }
          }
        }
      },
      delete = { itemId: LibraryItemId ->
        withContext(dispatcherProvider.databaseWrite) {
          db.libraryItemsQueries.deleteForId(itemId)
        }
      },
    ),
  ).build()

  override fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem> {
    return itemStore.stream(StoreReadRequest.cached(itemId, true))
      .mapNotNull { resp ->
        bark { "Library Item Store Response ($resp)" }
        resp.dataOrNull()
      }
  }

  override suspend fun getLibraryItem(itemId: LibraryItemId): LibraryItem {
    val cached = itemStore.stream(StoreReadRequest.cached(itemId, false))
      .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
      .firstOrNull()
      ?.dataOrNull()

    return if (cached != null && cached.media.tracks.isNotEmpty()) {
      cached
    } else {
      itemStore.fresh(itemId)
    }
  }
}

data class LibraryItemDbData(
  val audioFiles: List<MediaAudioFiles>,
  val audioTracks: List<MediaAudioTracks>,
  val chapters: List<MediaChapters>,
  val progress: MediaProgress?,
  val metadataAuthors: List<MetadataAuthor>,
)
