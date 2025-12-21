package app.campfire.search.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryId
import app.campfire.core.model.UserId
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.network.AudioBookShelfApi
import app.campfire.search.api.SearchResult
import kotlin.time.Duration.Companion.minutes
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object SearchStore : Cork {

  override val tag: String = "SearchStore"
  override val enabled: Boolean = false

  @Inject
  class Factory(
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    libraryItemDao: LibraryItemDao,
    urlHydrator: UrlHydrator,
    dispatcherProvider: DispatcherProvider,
  ) {
    private val fetcherFactory = SearchFetcherFactory(api)
    private val sourceOfTruthFactory = SearchSourceOfTruthFactory(
      db = db,
      libraryItemDao = libraryItemDao,
      urlHydrator = urlHydrator,
      dispatcherProvider = dispatcherProvider,
    )

    fun create(): Store<Operation.Query, SearchResult> {
      return StoreBuilder.from(
        fetcher = fetcherFactory.create(),
        sourceOfTruth = sourceOfTruthFactory.create(),
      ).cachePolicy(
        MemoryPolicy.builder<Operation.Query, SearchResult>()
          .setMaxSize(10)
          .setExpireAfterWrite(10.minutes)
          .build(),
      ).build()
    }
  }

  sealed interface Operation {
    data class Query(
      val userId: UserId,
      val libraryId: LibraryId,
      val text: String,
    ) : Operation {
      val databaseKey: String = "$userId::$libraryId::$text"
    }
  }
}
