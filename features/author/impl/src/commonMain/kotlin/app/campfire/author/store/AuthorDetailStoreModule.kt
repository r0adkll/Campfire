package app.campfire.author.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.Author
import app.campfire.core.model.AuthorId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Provides
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

class AuthorDetailStore(
  val store: Store<AuthorId, Author>,
)

@ContributesTo(UserScope::class)
interface AuthorDetailStoreModule {

  @OptIn(ExperimentalCoroutinesApi::class)
  @SingleIn(UserScope::class)
  @Provides
  fun provideAuthorDetailStore(
    userSession: UserSession,
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    coverImageHydrator: CoverImageHydrator,
    dispatcherProvider: DispatcherProvider,
  ): AuthorDetailStore {
    return StoreBuilder
      .from(
        fetcher = Fetcher.ofResult { authorId: AuthorId -> api.getAuthor(authorId).asFetcherResult() },
        sourceOfTruth = SourceOfTruth.of(
          reader = { authorId: AuthorId ->
            db.authorsQueries.selectForId(authorId)
              .asFlow()
              .mapToOne(dispatcherProvider.databaseRead)
              .flatMapLatest { author ->
                db.libraryItemsQueries
                  .selectForAuthorName(author.name)
                  .asFlow()
                  .mapToList(dispatcherProvider.databaseRead)
                  .map { libraryItems ->
                    val items = libraryItems
                      .map { it.asDomainModel(coverImageHydrator) }
                      .sortedBy { it.media.metadata.publishedYear?.toIntOrNull() }
                    author.asDomainModel(items)
                  }
              }
          },
          writer = { authorId, author ->
            val authorDbModel = author.asDbModel(coverImageHydrator)
            withContext(dispatcherProvider.databaseWrite) {
              db.transaction {
                db.authorsQueries.insert(authorDbModel)

                author.libraryItems?.forEach { libraryItem ->
                  val dbLibraryItem = libraryItem.asDbModel(userSession.serverUrl!!)
                  val dbMedia = libraryItem.media.asDbModel(dbLibraryItem.id)
                  db.libraryItemsQueries.insert(dbLibraryItem)
                  db.mediaQueries.insert(dbMedia)
                }
              }
            }
          },
          delete = { authorId ->
            withContext(dispatcherProvider.databaseWrite) {
              db.authorsQueries.deleteForId(authorId)
            }
          },
        ),
      )
      .build()
      .let { AuthorDetailStore(it) }
  }
}
