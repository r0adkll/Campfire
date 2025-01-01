package app.campfire.user.progress

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.logging.Extras
import app.campfire.core.logging.LogPriority
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.UserId
import app.campfire.network.AudioBookShelfApi
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.MutableStoreBuilder

object MediaProgressStore : Cork {

  override val tag: String = "MediaProgressStore"

  @Inject
  class Factory(
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    dispatcherProvider: DispatcherProvider,
  ) {

    private val fetcherFactory = MediaProgressFetcherFactory(api)
    private val sourceOfTruthFactory = MediaProgressSourceOfTruthFactory(db, dispatcherProvider)
    private val updaterFactory = MediaProgressUpdaterFactory(api)
    private val bookkeeperFactory = MediaProgressBookkeeperFactory(db)

    @OptIn(ExperimentalStoreApi::class)
    fun create(): MutableStore<Operation, Output> {
      return MutableStoreBuilder.from(
        fetcher = fetcherFactory.create(),
        sourceOfTruth = sourceOfTruthFactory.create(),
        converter = createConverter()
      ).build(
        updater = updaterFactory.create(),
        bookkeeper = bookkeeperFactory.create(),
      )
    }

    private fun createConverter(): Converter<Output, Output, Output> {
      return Converter.Builder<Output, Output, Output>()
        .fromOutputToLocal { it }
        .fromNetworkToLocal { it}
        .build()
    }
  }

  sealed interface Operation {

    sealed interface Query : Operation {
      data class One(val libraryItemId: LibraryItemId) : Query
      data class All(val userId: UserId) : Query
    }

    sealed interface Mutation : Operation {

      sealed interface Update : Mutation {
        data class UpsertOne(val item: MediaProgress) : Update
        data class UpsertMany(val items: List<MediaProgress>) : Update
      }

      sealed interface Delete : Mutation {
        data class One(
          val userId: UserId,
          val libraryItemId: LibraryItemId,
        ) : Delete
        // There is not a great way to conceivably do this right now
        // with the API, so lets not offer the option
        //data class All(val userId: UserId) : Delete
      }
    }

  }

  sealed class Output {
    data class Single(val item: MediaProgress) : Output()
    data class Collection(val items: List<MediaProgress>) : Output()

    fun requireSingle(): MediaProgress {
      return (this as Single).item
    }

    fun requireCollection(): List<MediaProgress> {
      return (this as Collection).items
    }
  }
}

