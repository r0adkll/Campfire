package app.campfire.search.store

import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.SearchResult as NetworkSearchResult
import app.campfire.search.store.SearchStore.Operation.Query
import org.mobilenativefoundation.store.store5.Fetcher

class SearchFetcherFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Fetcher<Query, NetworkSearchResult> {
    return Fetcher.ofResult { query ->
      api.searchLibrary(query.libraryId, query.text).asFetcherResult()
    }
  }
}
