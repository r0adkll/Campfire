package app.campfire.collections.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.Collection
import app.campfire.core.model.Collection as BookCollection
import app.campfire.core.model.CollectionId
import app.campfire.core.model.LibraryId
import app.campfire.core.model.UserId
import app.campfire.core.time.FatherTime
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.network.AudioBookShelfApi
import kotlin.uuid.Uuid
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.MutableStoreBuilder

object CollectionsStore : Cork {

  override val tag: String = "CollectionsStore"

  @Inject
  class Factory(
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    libraryItemDao: LibraryItemDao,
    tokenHydrator: TokenHydrator,
    fatherTime: FatherTime,
    userSessionManager: UserSessionManager,
    dispatcherProvider: DispatcherProvider,
  ) {
    private val fetcherFactory = CollectionsFetcherFactory(api, tokenHydrator)
    private val sourceOfTruthFactory =
      CollectionsSourceOfTruthFactory(
        db = db,
        libraryItemDao = libraryItemDao,
        dispatcherProvider = dispatcherProvider,
        tokenHydrator = tokenHydrator,
        fatherTime = fatherTime,
      )
    private val updaterFactory = CollectionsUpdaterFactory(api, db, dispatcherProvider)
    private val bookkeeperFactory = CollectionsBookKeeperFactory(userSessionManager, db, dispatcherProvider)

    @OptIn(ExperimentalStoreApi::class)
    fun create(): MutableStore<Operation, Output> {
      return MutableStoreBuilder.from(
        fetcher = fetcherFactory.create(),
        sourceOfTruth = sourceOfTruthFactory.create(),
        converter = createConverter(),
      ).build(
        updater = updaterFactory.create(),
        bookkeeper = bookkeeperFactory.create(),
      )
    }

    private fun createConverter(): Converter<List<Collection>, List<Collection>, Output> {
      return Converter.Builder<List<Collection>, List<Collection>, Output>()
        // We pre-convert these in the fetcher so we can perform the token hydration
        // in a coroutine rather than using runBlocking { } here.
        .fromNetworkToLocal { network -> network }
        .fromOutputToLocal { output ->
          when (output) {
            is Output.Single -> listOf(output.collection)
            is Output.Collection -> output.collections
          }
        }
        .build()
    }
  }

  sealed interface Operation {
    data class All(
      val userId: UserId,
      val libraryId: LibraryId,
    ) : Operation

    data class Single(
      val userId: UserId,
      val libraryId: LibraryId,
      val collectionId: CollectionId,
    ) : Operation

    sealed interface Mutation : Operation {
      data class Create(
        val userId: UserId,
        val name: String,
        val description: String?,
        val libraryId: LibraryId,
        val bookIds: List<String>,
        val creationId: Uuid = Uuid.random(),
      ) : Mutation

      data class Update(
        val userId: UserId,
        val id: CollectionId,
        val name: String?,
        val description: String?,
      ) : Mutation

      data class Delete(
        val userId: UserId,
        val id: CollectionId,
      ) : Mutation
    }
  }

  sealed interface Output {
    fun isEmpty(): Boolean

    data class Single(val collection: BookCollection) : Output {
      override fun isEmpty(): Boolean = false
    }

    data class Collection(val collections: List<BookCollection>) : Output {
      override fun isEmpty(): Boolean = collections.isEmpty()
    }
  }

  sealed interface Update {
    data object Success : Update
    data object Failure : Update
  }
}
