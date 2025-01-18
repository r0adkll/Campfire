package app.campfire.search.api

import kotlinx.coroutines.flow.Flow

interface SearchRepository {

  fun searchCurrentLibrary(query: String): Flow<SearchResult>
}
