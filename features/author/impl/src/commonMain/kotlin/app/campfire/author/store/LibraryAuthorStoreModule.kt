package app.campfire.author.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryId
import app.campfire.data.Authors
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Provides
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

class LibraryAuthorStore(
  val store: Store<LibraryId, List<Authors>>,
)

@ContributesTo(UserScope::class)
interface LibraryAuthorStoreModule {

  @SingleIn(UserScope::class)
  @Provides
  fun provideAuthorsStore(
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    imageHydrator: UrlHydrator,
    dispatcherProvider: DispatcherProvider,
  ): LibraryAuthorStore {
    return StoreBuilder
      .from(
        fetcher = Fetcher.ofResult { libraryId: LibraryId -> api.getAuthors(libraryId).asFetcherResult() },
        sourceOfTruth = SourceOfTruth.of(
          reader = { libraryId: LibraryId ->
            db.authorsQueries.selectForLibrary(libraryId)
              .asFlow()
              .mapToList(dispatcherProvider.databaseRead)
          },
          writer = { libraryId: LibraryId, authors ->
            val dbAuthors = authors.map { it.asDbModel(imageHydrator) }
            withContext(dispatcherProvider.databaseWrite) {
              db.transaction {
                dbAuthors.forEach { db.authorsQueries.insert(it) }
              }
            }
          },
          delete = { libraryId: LibraryId ->
            withContext(dispatcherProvider.databaseWrite) {
              db.authorsQueries.deleteForLibrary(libraryId)
            }
          },
        ),
      )
      .cachePolicy(
        MemoryPolicy.builder<LibraryId, List<Authors>>()
          .setMaxSize(50)
          .setExpireAfterAccess(10.minutes)
          .build(),
      )
      .build()
      .let { LibraryAuthorStore(it) }
  }
}
