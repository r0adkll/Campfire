// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.socket

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryId
import app.campfire.core.model.SeriesId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.data.mapping.asDbModel
import app.campfire.network.models.Series
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Handles `Series*` socket events by writing through to the local DB.
 *
 * Wire `Series` lacks the user-scoped flags (`inProgress`, `hasActiveBook`, …) that the
 * personalized-series fetch carries. To avoid clobbering those on an update we use
 * `update` (which only sets the columns the wire model owns) then `insertOrIgnore`
 * so the row is created if missing.
 */
interface SeriesEventHandler {
  suspend fun onSeriesAdded(series: Series)
  suspend fun onSeriesUpdated(series: Series)
  suspend fun onSeriesRemoved(seriesId: SeriesId, libraryId: LibraryId)
}

@ContributesBinding(UserScope::class)
@Inject
class DefaultSeriesEventHandler(
  private val db: CampfireDatabase,
  private val userSession: UserSession,
  private val dispatcherProvider: DispatcherProvider,
) : SeriesEventHandler {

  override suspend fun onSeriesAdded(series: Series) = upsert(series)

  override suspend fun onSeriesUpdated(series: Series) = upsert(series)

  override suspend fun onSeriesRemoved(seriesId: SeriesId, libraryId: LibraryId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.seriesQueries.deleteById(id = seriesId, libraryId = libraryId)
    }
  }

  private suspend fun upsert(series: Series) {
    val userId = userSession.userId ?: return
    val libraryId = series.libraryId ?: return
    val dbModel = series.asDbModel(userId, libraryId)
    withContext(dispatcherProvider.databaseWrite) {
      db.seriesQueries.update(
        name = dbModel.name,
        description = dbModel.description,
        addedAt = dbModel.addedAt,
        updatedAt = dbModel.updatedAt,
        inProgress = dbModel.inProgress,
        hasActiveBook = dbModel.hasActiveBook,
        hideFromContinueListening = dbModel.hideFromContinueListening,
        bookInProgressLastUpdate = dbModel.bookInProgressLastUpdate,
        firstBookUnreadId = dbModel.firstBookUnreadId,
        libraryId = dbModel.libraryId,
        id = dbModel.id,
      )
      db.seriesQueries.insertOrIgnore(dbModel)
    }
  }
}
