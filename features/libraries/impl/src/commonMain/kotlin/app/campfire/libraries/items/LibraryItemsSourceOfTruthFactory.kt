package app.campfire.libraries.items

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.data.mapping.asDbModel
import app.campfire.libraries.db.FilteredItemQueryHelper
import app.campfire.libraries.items.LibraryItemsStore.Query
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MinifiedBookMetadata
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

class LibraryItemsSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val userSession: UserSession,
  private val filteredItemQueryHelper: FilteredItemQueryHelper,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create() = SourceOfTruth.of(
    reader = { query: Query ->
      filteredItemQueryHelper.select(
        filter = query.filter,
        sortMode = query.sortMode,
        sortDirection = query.sortDirection,
        libraryId = query.libraryId,
      ).asFlow()
        .catch {
          bark(LogPriority.ERROR, throwable = it) { "Error while reading library items" }
        }
        .mapToList(dispatcherProvider.databaseRead)
    },
    writer = { query, networkItems: List<LibraryItemMinified<MinifiedBookMetadata>> ->
      withContext(dispatcherProvider.databaseWrite) {
        db.transaction {
          networkItems.forEach { item ->
            val libraryItem = item.asDbModel(userSession.serverUrl!!)
            val media = item.media.asDbModel(item.id)

            db.libraryItemsQueries.insertOrIgnore(libraryItem)
            db.mediaQueries.insertOrIgnore(media)
          }
        }
      }
    },
    delete = { query ->
      withContext(dispatcherProvider.databaseWrite) {
        db.libraryItemsQueries.deleteForLibrary(query.libraryId)
      }
    },
  )
}
