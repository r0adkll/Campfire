package app.campfire.libraries

import app.campfire.CampfireDatabase
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.UserScope
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.mapping.asDbModels
import app.campfire.network.AudioBookShelfApi
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder

@ContributesBinding(UserScope::class)
@Inject
class StoreLibraryRepository(
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val settings: CampfireSettings,
  private val dispatcherProvider: DispatcherProvider,
) : LibraryRepository {

  private val libraryItemStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { libraryId: LibraryId ->
      api.getLibraryItems(libraryId).asFetcherResult()
    },
    sourceOfTruth = SourceOfTruth.of(
      reader = { libraryId: LibraryId ->
        db.libraryItemsQueries.selectForLibrary(libraryId)
          .asFlow()
          .mapToList(dispatcherProvider.io)
      },
      writer = { _, data ->
        db.transaction {
          data.forEach { item ->
            val (libraryItem, media) = item.asDbModels("" /*TODO: Inject Server URL here*/)
            db.libraryItemsQueries.insert(libraryItem)
            db.mediaQueries.insert(media)
          }
        }
      },
      delete = { libraryId: LibraryId ->
        db.libraryItemsQueries.deleteForLibrary(libraryId)
      },
      deleteAll = {
        // Unsupported
      },
    ),
  ).build()

  override fun observeAllLibraries(): Flow<List<Library>> {
    TODO("Not yet implemented")
  }

  override fun observeLibraryItems(libraryId: LibraryId): Flow<List<LibraryItem>> {
    val currentServerUrl = settings.currentServerUrl
      ?: throw IllegalStateException("You must be logged in to view library items")

    TODO("Not yet implemented")
  }
}

fun <T : Any> Result<T>.asFetcherResult(): FetcherResult<T> {
  return when {
    isSuccess -> FetcherResult.Data(getOrThrow(), "api")
    else -> FetcherResult.Error.Exception(exceptionOrNull()!!)
  }
}
