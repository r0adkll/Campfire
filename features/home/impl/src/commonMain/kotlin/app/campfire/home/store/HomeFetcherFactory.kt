package app.campfire.home.store

import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.Shelf
import org.mobilenativefoundation.store.store5.Fetcher

class HomeFetcherFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Fetcher<HomeStore.Key, List<Shelf>> {
    return Fetcher.ofResult { key ->
      api.getPersonalizedHome(key.libraryId).asFetcherResult()
    }
  }
}
