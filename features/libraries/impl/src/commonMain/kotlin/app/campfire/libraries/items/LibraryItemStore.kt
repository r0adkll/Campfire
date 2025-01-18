package app.campfire.libraries.items

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.network.AudioBookShelfApi
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object LibraryItemStore : Cork {

  override val tag: String = "LibraryItemStore"

  @Inject
  class Factory(
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    libraryItemDao: LibraryItemDao,
    dispatcherProvider: DispatcherProvider,
  ) {

    private val fetcherFactory = LibraryItemFetcherFactory(api)
    private val sourceOfTruthFactory = LibraryItemSourceOfTruthFactory(db, libraryItemDao, dispatcherProvider)

    fun create(): Store<LibraryItemId, LibraryItem> {
      return StoreBuilder.from(
        fetcher = fetcherFactory.create(),
        sourceOfTruth = sourceOfTruthFactory.create(),
      ).cachePolicy(
        MemoryPolicy.builder<LibraryItemId, LibraryItem>()
          .setMaxSize(100)
          .build(),
      ).build()
    }
  }
}
