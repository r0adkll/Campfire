package app.campfire.libraries.items

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryId
import app.campfire.core.session.UserSession
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import app.campfire.data.mapping.model.LibraryItemWithMedia
import app.campfire.libraries.api.LibraryItemFilter
import app.campfire.libraries.db.FilteredItemQueryHelper
import app.campfire.network.AudioBookShelfApi
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object LibraryItemsStore : Cork {

  override val tag: String = "LibraryItemsStore"

  @Inject
  class Factory(
    userSession: UserSession,
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    dispatcherProvider: DispatcherProvider,
    filteredItemQueryHelper: FilteredItemQueryHelper,
  ) {

    private val fetcherFactory = LibraryItemsFetcherFactory(api)
    private val sourceOfTruthFactory = LibraryItemsSourceOfTruthFactory(
      db = db,
      userSession = userSession,
      filteredItemQueryHelper = filteredItemQueryHelper,
      dispatcherProvider = dispatcherProvider,
    )

    fun create(): Store<Query, List<LibraryItemWithMedia>> {
      return StoreBuilder.from(
        fetcher = fetcherFactory.create(),
        sourceOfTruth = sourceOfTruthFactory.create(),
      ).cachePolicy(
        MemoryPolicy.builder<Query, List<LibraryItemWithMedia>>()
          .setMaxSize(200)
          .build(),
      ).build()
    }
  }

  data class Query(
    val libraryId: LibraryId,
    val filter: LibraryItemFilter?,
    val sortMode: SortMode,
    val sortDirection: SortDirection,
  )
}
