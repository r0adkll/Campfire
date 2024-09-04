package app.campfire.libraries

import app.campfire.CampfireDatabase
import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.LibraryItemExpanded
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder

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

  private val itemStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { itemId: LibraryItemId -> api.getLibraryItem(itemId).asFetcherResult() },
    sourceOfTruth = SourceOfTruth.of(
      reader = { itemId: LibraryItemId ->
        TODO()
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
            itemExpanded.media.audioFiles.forEach { audioFile ->
              db.mediaAudioFilesQueries.insert(audioFile.asDbModel(media.mediaId))
            }

            itemExpanded.media.chapters.forEach { chapter ->
              db.mediaChaptersQueries.insert(chapter.asDbModel(media.mediaId))
            }

            itemExpanded.media.tracks.forEach { track ->
              db.mediaAudioTracksQueries.insert(track.asDbModel(media.mediaId))
            }
          }
        }
      },
      delete = { itemId: LibraryItemId ->
        withContext(dispatcherProvider.databaseWrite) {
          db.libraryItemsQueries.deleteForId(itemId)
        }
      }
    )
  )

  override fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem> {
    TODO("Not yet implemented")
  }
}
