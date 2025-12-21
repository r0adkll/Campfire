package app.campfire.series.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryId
import app.campfire.core.model.Series
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.network.AudioBookShelfApi
import kotlin.time.Duration.Companion.minutes
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.cache5.Cache
import org.mobilenativefoundation.store.cache5.CacheBuilder
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object SeriesStore {

  data class Key(
    val userId: UserId,
    val libraryId: LibraryId,
  )

  class Factory(
    userSession: UserSession,
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    urlHydrator: UrlHydrator,
    dispatcherProvider: DispatcherProvider,
    private val cache: Cache<Key, List<Series>>,
  ) {

    @Inject
    constructor(
      userSession: UserSession,
      api: AudioBookShelfApi,
      db: CampfireDatabase,
      urlHydrator: UrlHydrator,
      dispatcherProvider: DispatcherProvider,
    ) : this(
      userSession = userSession,
      api = api,
      db = db,
      urlHydrator = urlHydrator,
      dispatcherProvider = dispatcherProvider,
      cache = CacheBuilder<Key, List<Series>>()
        .expireAfterAccess(5.minutes)
        .build(),
    )

    private val seriesFetcherFactory = SeriesFetcherFactory(api)
    private val seriesSourceOfTruthFactory =
      SeriesSourceOfTruthFactory(userSession, db, urlHydrator, dispatcherProvider)

    fun create(): Store<Key, List<Series>> {
      return StoreBuilder
        .from(
          fetcher = seriesFetcherFactory.create(),
          sourceOfTruth = seriesSourceOfTruthFactory.create(),
          memoryCache = cache,
        )
        .build()
    }
  }
}
