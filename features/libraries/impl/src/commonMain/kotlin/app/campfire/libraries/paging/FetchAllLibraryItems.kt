package app.campfire.libraries.paging

import app.campfire.core.model.LibraryId
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.PagedResponse
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.nextPage

/**
 * Page through the server's minified library item listing until exhaustion, accumulating
 * every item in the library.
 */
suspend fun fetchAllLibraryItems(
  api: AudioBookShelfApi,
  libraryId: LibraryId,
  pageSize: Int = DEFAULT_FETCH_PAGE_SIZE,
): Result<List<LibraryItemMinified>> {
  return fetchAllPages { page ->
    api.getLibraryItemsMinified(
      libraryId = libraryId,
      page = page,
      limit = pageSize,
    )
  }
}

/**
 * Accumulate every page of a paged endpoint starting from page 0, following [nextPage]
 * until the server reports exhaustion. [maxPages] bounds misbehaving servers so a bad
 * `nextPage` chain can't loop forever — hitting the cap returns what was accumulated.
 */
internal suspend fun <T> fetchAllPages(
  maxPages: Int = MAX_FETCH_PAGES,
  fetchPage: suspend (page: Int) -> Result<PagedResponse<T>>,
): Result<List<T>> {
  val items = mutableListOf<T>()
  var page = 0
  repeat(maxPages) {
    val response = fetchPage(page).getOrElse { return Result.failure(it) }
    items += response.data
    page = response.nextPage ?: return Result.success(items)
  }
  return Result.success(items)
}

private const val DEFAULT_FETCH_PAGE_SIZE = 100
private const val MAX_FETCH_PAGES = 50
