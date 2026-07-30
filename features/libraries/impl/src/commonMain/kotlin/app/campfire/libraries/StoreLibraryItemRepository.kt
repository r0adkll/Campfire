// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries

import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.item.LibraryItemStore
import app.campfire.network.AudioBookShelfApi
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
  private val api: AudioBookShelfApi,
) : LibraryItemRepository {

  private val itemStore = libraryItemStoreFactory.create()

  override fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem> {
    return itemStore.stream(StoreReadRequest.cached(itemId, true))
      .mapNotNull { resp ->
        if (resp is StoreReadResponse.Error.Exception) {
          bark(throwable = resp.error) { "Library Item Store Response Error" }
        }
        resp.dataOrNull()
      }
  }

  override suspend fun getLibraryItem(itemId: LibraryItemId): LibraryItem {
    val cached = itemStore.stream(StoreReadRequest.cached(itemId, false))
      .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
      .firstOrNull()
      ?.dataOrNull()

    return if (cached != null && cached.isHydratedForPlayback) {
      cached
    } else {
      itemStore.fresh(itemId)
    }
  }

  override suspend fun deleteLibraryItem(itemId: LibraryItemId, hardDelete: Boolean): Result<Unit> {
    return api.deleteLibraryItem(itemId, hardDelete)
      .onSuccess { itemStore.clear(itemId) }
  }

  /**
   * Whether the cached item carries enough relational data to be played without a refetch.
   * For books that means item-level audio tracks are present. Podcasts attach tracks per
   * episode (the item's `media.tracks` is always empty), so we instead check that every
   * episode has its `audioTrack` hydrated — that's the signal we got the expanded shape
   * back from the server rather than a basic-shape stub.
   */
  private val LibraryItem.isHydratedForPlayback: Boolean
    get() = when (val media = media) {
      is Media.Book -> media.tracks.isNotEmpty()
      is Media.Podcast -> media.episodes.isNotEmpty() && media.episodes.all { it.audioTrack != null }
    }
}
