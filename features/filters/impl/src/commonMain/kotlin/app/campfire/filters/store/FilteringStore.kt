package app.campfire.filters.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.FilterData
import app.campfire.core.model.LibraryId
import app.campfire.core.time.FatherTime
import app.campfire.network.AudioBookShelfApi
import kotlin.time.Duration.Companion.minutes
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

class FilteringStore private constructor() {

  @Inject
  class Factory(
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    dispatcherProvider: DispatcherProvider,
    fatherTime: FatherTime,
  ) {

    private val filterFetcherFactory = FilterFetcherFactory(api)
    private val filterSourceOfTruthFactory = FilterSourceOfTruthFactory(db, dispatcherProvider, fatherTime)

    fun create(): Store<LibraryId, FilterData> = StoreBuilder.Companion
      .from(
        fetcher = filterFetcherFactory.create(),
        sourceOfTruth = filterSourceOfTruthFactory.create(),
      )
      .cachePolicy(
        MemoryPolicy.Companion.builder<LibraryId, FilterData>()
          .setMaxSize(3)
          .setExpireAfterAccess(5.minutes)
          .build(),
      ).build()
  }
}
