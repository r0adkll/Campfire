// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries

import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.libraries.api.LibraryItemPurger
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.item.LibraryItemStore
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.isNotFound
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.CancellationException
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
  private val purger: LibraryItemPurger,
) : LibraryItemRepository {

  private val itemStore = libraryItemStoreFactory.create()

  override fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem> {
    return itemStore.stream(StoreReadRequest.cached(itemId, true))
      .mapNotNull { resp ->
        if (resp is StoreReadResponse.Error.Exception) {
          bark(throwable = resp.error) { "Library Item Store Response Error" }
          // The item is gone from the server: drop the stale cached copy and end the stream
          // with the error so the screen stops showing it.
          if (resp.error.isNotFound) {
            purger.purge(listOf(itemId))
            throw resp.error
          }
        }
        resp.dataOrNull()
      }
  }

  override suspend fun getLibraryItem(itemId: LibraryItemId): LibraryItem {
    val cached = itemStore.stream(StoreReadRequest.cached(itemId, false))
      .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
      .firstOrNull()
      ?.dataOrNull()

    if (cached != null && cached.isHydratedForPlayback) return cached

    return try {
      itemStore.fresh(itemId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (e.isNotFound) {
        purger.purge(listOf(itemId))
        throw e
      }

      // The server may be unreachable (offline, DNS failure, timeout). Prefer a partially-hydrated
      // cached copy over crashing the caller; only propagate when we have nothing at all to hand back.
      if (cached != null) {
        bark(throwable = e) { "Failed to refresh library item $itemId, falling back to cached copy" }
        cached
      } else {
        throw e
      }
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
