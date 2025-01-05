package app.campfire.user.mediaprogress.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.UserId
import app.campfire.network.AudioBookShelfApi
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

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

    fun create(): Store<Operation, Output> {
      return StoreBuilder
        .from(
          fetcher = fetcherFactory.create(),
          sourceOfTruth = sourceOfTruthFactory.create(),
        )
        .cachePolicy(
          MemoryPolicy.builder<Operation, Output>()
            .setMaxSize(10)
            .build(),
        )
        .build()
    }
  }

  sealed interface Operation {
    sealed interface Query : Operation {
      data class One(val userId: UserId, val libraryItemId: LibraryItemId) : Query
      data class All(val userId: UserId) : Query
    }
  }

  sealed class Output {
    data class Single(val item: MediaProgress?) : Output()
    data class Collection(val items: List<MediaProgress>) : Output()

    fun requireSingle(): MediaProgress? {
      return (this as Single).item
    }

    fun requireCollection(): List<MediaProgress> {
      return (this as Collection).items
    }
  }
}
