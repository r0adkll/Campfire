package app.campfire.home.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryId
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.home.api.model.Shelf
import app.campfire.network.AudioBookShelfApi
import kotlin.time.Duration.Companion.minutes
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object HomeStore : Cork {

  override val tag: String = "HomeStore"

  @Inject
  class Factory(
    api: AudioBookShelfApi,
    private val db: CampfireDatabase,
    private val imageHydrator: TokenHydrator,
    private val libraryItemDao: LibraryItemDao,
    private val dispatcherProvider: DispatcherProvider,
  ) {

    private val fetcherFactory = HomeFetcherFactory(api)
    private val sourceOfTruthFactory = HomeSourceOfTruthFactory(
      db = db,
      imageHydrator = imageHydrator,
      libraryItemDao = libraryItemDao,
      dispatcherProvider = dispatcherProvider,
    )

    fun create(): Store<Key, List<Shelf<*>>> {
      return StoreBuilder
        .from(
          fetcher = fetcherFactory.create(),
          sourceOfTruth = sourceOfTruthFactory.create(),
        )
        .cachePolicy(
          MemoryPolicy.builder<Key, List<Shelf<*>>>()
            .setExpireAfterAccess(5.minutes)
            .build(),
        )
        .build()
    }
  }

  data class Key(val libraryId: LibraryId)
}
