// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.socket

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.data.mapping.asDbModel
import app.campfire.network.models.Library
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Handles library-scoped socket events by writing through to the local DB.
 * Mirrors the upsert pattern used by `StoreLibraryRepository`'s `SourceOfTruth.writer`:
 * `update` then `insert` so the row is created if missing or updated if present.
 */
interface LibraryEventHandler {
  suspend fun onLibraryAdded(library: Library)
  suspend fun onLibraryUpdated(library: Library)
  suspend fun onLibraryRemoved(libraryId: LibraryId)
}

@ContributesBinding(UserScope::class)
@Inject
class DefaultLibraryEventHandler(
  private val db: CampfireDatabase,
  private val userSession: UserSession,
  private val dispatcherProvider: DispatcherProvider,
) : LibraryEventHandler {

  override suspend fun onLibraryAdded(library: Library) {
    upsert(library)
  }

  override suspend fun onLibraryUpdated(library: Library) {
    upsert(library)
  }

  override suspend fun onLibraryRemoved(libraryId: LibraryId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.librariesQueries.deleteById(libraryId)
    }
  }

  private suspend fun upsert(library: Library) {
    val userId = userSession.userId ?: return
    val dbModel = library.asDbModel(userId)
    withContext(dispatcherProvider.databaseWrite) {
      db.librariesQueries.update(
        id = dbModel.id,
        name = dbModel.name,
        displayOrder = dbModel.displayOrder,
        icon = dbModel.icon,
        mediaType = dbModel.mediaType,
        provider = dbModel.provider,
        createdAt = dbModel.createdAt,
        lastUpdate = dbModel.lastUpdate,
        coverAspectRatio = dbModel.coverAspectRatio,
        audiobooksOnly = dbModel.audiobooksOnly,
      )
      db.librariesQueries.insert(dbModel)
    }
  }
}
