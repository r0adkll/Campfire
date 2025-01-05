package app.campfire.libraries

import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.data.MediaAudioFiles
import app.campfire.data.MediaAudioTracks
import app.campfire.data.MediaChapters
import app.campfire.data.MetadataAuthor
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.items.LibraryItemStore
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.impl.extensions.fresh

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreLibraryItemRepository(
  libraryItemStoreFactory: LibraryItemStore.Factory,
) : LibraryItemRepository {

  private val itemStore = libraryItemStoreFactory.create()

  override fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem> {
    return itemStore.stream(StoreReadRequest.cached(itemId, true))
      .mapNotNull { resp ->
        if (resp is StoreReadResponse.Error.Exception) {
          bark(throwable = resp.error) { " Library Item Store Response Error " }
        }

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
  val metadataAuthors: List<MetadataAuthor>,
)
