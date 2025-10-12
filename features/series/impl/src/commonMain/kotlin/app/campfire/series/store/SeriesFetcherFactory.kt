package app.campfire.series.store

import app.campfire.core.model.LibraryId
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import org.mobilenativefoundation.store.store5.Fetcher

internal class SeriesFetcherFactory(val api: AudioBookShelfApi) {

  fun create() = Fetcher.ofResult { libraryId: LibraryId ->
    api.getSeries(libraryId).asFetcherResult()
  }
}
