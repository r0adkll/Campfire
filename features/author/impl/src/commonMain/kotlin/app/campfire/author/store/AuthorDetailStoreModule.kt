// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.author.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.Author
import app.campfire.core.model.AuthorId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.core.session.serverUrl
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.data.mapping.model.mapToLibraryItemWithProgress
import app.campfire.libraries.api.LibraryItemPurger
import app.campfire.network.AudioBookShelfApi
import app.cash.sqldelight.async.coroutines.awaitAsList
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
    urlHydrator: UrlHydrator,
    dispatcherProvider: DispatcherProvider,
    purger: LibraryItemPurger,
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
                  .selectForAuthorName(
                    userId = userSession.requiredUserId,
                    authorName = author.name,
                    mapper = ::mapToLibraryItemWithProgress,
                  )
                  .asFlow()
                  .mapToList(dispatcherProvider.databaseRead)
                  .map { libraryItems ->
                    val items = libraryItems
                      .map { it.asDomainModel(urlHydrator) }
                      .sortedBy { it.media.metadata.publishedYear?.toIntOrNull() }
                    author.asDomainModel(items)
                  }
              }
          },
          writer = { authorId, author ->
            val authorDbModel = author.asDbModel(urlHydrator)
            withContext(dispatcherProvider.databaseWrite) {
              db.transaction {
                db.authorsQueries.insert(authorDbModel)

                author.libraryItems?.forEach { libraryItem ->
                  val dbLibraryItem = libraryItem.asDbModel(userSession.serverUrl!!)
                  val dbMedia = libraryItem.media.asDbModel(dbLibraryItem.id)

                  // Various APIs return minified versions of the library item queries, which if
                  // persisted as INSERT OR REPLACE will overwrite the expanded version stored
                  // including a lot of useful information for display rich item details such as
                  // chapters, tracks, author lists, and narrator lists.
                  db.libraryItemsQueries.insertOrIgnore(dbLibraryItem)
                  db.mediaQueries.insertOrIgnore(dbMedia)
                }
              }
            }

            // The detail response lists every book by this author, so a cached book the
            // reader would show that isn't in it may have been removed from the server.
            author.libraryItems?.let { libraryItems ->
              val serverIds = libraryItems.mapTo(HashSet()) { it.id }
              val cachedIds = withContext(dispatcherProvider.databaseRead) {
                db.libraryItemsQueries
                  .selectIdsForAuthorName(authorName = author.name, libraryId = author.libraryId)
                  .awaitAsList()
              }
              purger.purgeIfRemoved(cachedIds.filterNot { it in serverIds })
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
