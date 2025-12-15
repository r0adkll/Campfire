package app.campfire.series.store

import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import org.mobilenativefoundation.store.store5.Fetcher

internal class SeriesFetcherFactory(val api: AudioBookShelfApi) {

  fun create() = Fetcher.ofResult { key: SeriesStore.Key ->
    api.getSeries(key.libraryId).asFetcherResult()
  }
}
