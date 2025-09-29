package app.campfire.libraries.items

import app.campfire.core.settings.SortDirection
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.LibraryItemFilter
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MinifiedBookMetadata
import org.mobilenativefoundation.store.store5.Fetcher

class LibraryItemsFetcherFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Fetcher<LibraryItemsStore.Query, List<LibraryItemMinified<MinifiedBookMetadata>>> {
    return Fetcher.ofResult { query ->
      api.getLibraryItemsMinified(
        libraryId = query.libraryId,
        filter = query.filter?.let {
          LibraryItemFilter(it.group, it.value)
        },
        sortMode = query.sortMode.networkKey,
        sortDescending = query.sortDirection == SortDirection.Descending,
      ).map { it.data }.asFetcherResult()
    }
  }
}
