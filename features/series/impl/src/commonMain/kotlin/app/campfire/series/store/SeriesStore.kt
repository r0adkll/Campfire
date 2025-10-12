package app.campfire.series.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryId
import app.campfire.core.session.UserSession
import app.campfire.data.SelectForSeries
import app.campfire.data.Series
import app.campfire.network.AudioBookShelfApi
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object SeriesStore {

  @Inject
  class Factory(
    userSession: UserSession,
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    dispatcherProvider: DispatcherProvider,
  ) {
    private val seriesFetcherFactory = SeriesFetcherFactory(api)
    private val seriesSourceOfTruthFactory = SeriesSourceOfTruthFactory(userSession, db, dispatcherProvider)

    fun create(): Store<LibraryId, Map<Series, List<SelectForSeries>>> {
      return StoreBuilder.from(
        fetcher = seriesFetcherFactory.create(),
        sourceOfTruth = seriesSourceOfTruthFactory.create(),
      ).build()
    }
  }
}
