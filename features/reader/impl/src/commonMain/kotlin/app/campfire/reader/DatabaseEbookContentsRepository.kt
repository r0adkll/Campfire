// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.data.EbookContents
import app.campfire.reader.api.EbookContentsEntry
import app.campfire.reader.api.EbookContentsRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@ContributesBinding(UserScope::class)
@Inject
class DatabaseEbookContentsRepository(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : EbookContentsRepository {

  override fun observeContents(itemId: LibraryItemId): Flow<List<EbookContentsEntry>> =
    db.ebookContentsQueries.selectForItem(itemId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { rows -> rows.map { it.asEntry() } }

  override suspend fun saveContents(itemId: LibraryItemId, entries: List<EbookContentsEntry>) {
    withContext(dispatcherProvider.databaseWrite) {
      db.transaction {
        db.ebookContentsQueries.deleteForItem(itemId)
        entries.forEachIndexed { position, entry ->
          db.ebookContentsQueries.insert(
            EbookContents(
              libraryItemId = itemId,
              position = position,
              label = entry.label,
              href = entry.href,
              depth = entry.depth,
              fraction = entry.fraction?.toDouble(),
            ),
          )
        }
      }
    }
  }
}

private fun EbookContents.asEntry() = EbookContentsEntry(
  label = label,
  href = href,
  depth = depth,
  fraction = fraction?.toFloat(),
)
