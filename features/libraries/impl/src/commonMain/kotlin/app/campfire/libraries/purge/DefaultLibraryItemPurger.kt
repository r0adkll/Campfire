// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.purge

import app.campfire.CampfireDatabase
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Corked
import app.campfire.core.model.LibraryItemId
import app.campfire.libraries.api.LibraryItemPurger
import app.campfire.network.ApiException
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.isNotFound
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class DefaultLibraryItemPurger(
  private val db: CampfireDatabase,
  private val api: AudioBookShelfApi,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val dispatcherProvider: DispatcherProvider,
) : LibraryItemPurger {

  companion object : Corked("LibraryItemPurger")

  override suspend fun purge(itemIds: Collection<LibraryItemId>) {
    if (itemIds.isEmpty()) return
    ibark { "Purging ${itemIds.size} removed item(s): $itemIds" }

    withContext(dispatcherProvider.databaseWrite) {
      db.transaction {
        // Grouped statements return a lazy result that only runs once awaited
        itemIds.forEach { db.libraryItemsQueries.purgeForId(it).await() }
      }
    }

    itemIds.forEach { itemId ->
      try {
        offlineDownloadManager.deleteAllForItemId(itemId)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        ebark(e) { "Failed to remove downloads for purged item $itemId" }
      }
    }
  }

  override suspend fun purgeIfRemoved(candidates: Collection<LibraryItemId>): Set<LibraryItemId> {
    val removed = mutableSetOf<LibraryItemId>()
    for (itemId in candidates) {
      val error = api.getLibraryItem(itemId).exceptionOrNull() ?: continue
      when {
        error.isNotFound -> removed += itemId
        // The server answered but didn't confirm removal (e.g. 403), so the item stays.
        error is ApiException -> continue
        // No answer at all (offline, timeout): the rest would fail the same way.
        else -> break
      }
    }
    purge(removed)
    return removed
  }
}
