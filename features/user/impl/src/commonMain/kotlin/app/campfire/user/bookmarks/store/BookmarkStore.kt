package app.campfire.user.bookmarks.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.UserId
import app.campfire.core.time.FatherTime
import app.campfire.data.mapping.asDomainModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.User
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.MutableStoreBuilder

object BookmarkStore : Cork {

  override val tag: String = "BookmarkStore"
  override val enabled: Boolean = false

  @Inject
  class Factory(
    userSessionManager: UserSessionManager,
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    dispatcherProvider: DispatcherProvider,
    fatherTime: FatherTime,
  ) {
    private val fetcherFactory = BookmarkFetcherFactory(api)
    private val sourceOfTruthFactory = BookmarkSourceOfTruthFactory(db, dispatcherProvider, fatherTime)
    private val updaterFactory = BookmarkUpdaterFactory(api)
    private val bookkeeperFactory = BookmarkBookkeeperFactory(userSessionManager, db, dispatcherProvider)

    @OptIn(ExperimentalStoreApi::class)
    fun create(): MutableStore<Operation, List<Bookmark>> {
      return MutableStoreBuilder.from(
        fetcher = fetcherFactory.create(),
        sourceOfTruth = sourceOfTruthFactory.create(),
        converter = createConverter(),
      ).build(
        updater = updaterFactory.create(),
        bookkeeper = bookkeeperFactory.create(),
      )
    }

    private fun createConverter(): Converter<User, List<Bookmark>, List<Bookmark>> {
      return Converter.Builder<User, List<Bookmark>, List<Bookmark>>()
        .fromNetworkToLocal { user -> user.bookmarks.map { it.asDomainModel(user.id) } }
        .fromOutputToLocal { it }
        .build()
    }
  }

  sealed interface Operation {
    data class Item(
      val userId: UserId,
      val libraryItemId: LibraryItemId,
    ) : Operation

    sealed interface Mutation : Operation {
      data class Create(
        val userId: UserId,
        val libraryItemId: LibraryItemId,
        val timeInSeconds: Int,
        val title: String,
      ) : Mutation

      data class Delete(
        val userId: UserId,
        val libraryItemId: LibraryItemId,
        val timeInSeconds: Int,
      ) : Mutation
    }
  }

  sealed interface Update {
    data object Success : Update
    data object Failure : Update
  }
}
