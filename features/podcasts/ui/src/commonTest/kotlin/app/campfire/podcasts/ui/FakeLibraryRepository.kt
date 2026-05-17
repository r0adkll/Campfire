package app.campfire.podcasts.ui

import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.paging.LibraryItemPager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

class FakeLibraryRepository(
  var addPodcastContextResult: Result<AddPodcastContext> = Result.success(
    AddPodcastContext(folders = emptyList(), searchRegion = null),
  ),
) : LibraryRepository {

  val addPodcastContextCalls = mutableListOf<LibraryId>()

  override fun observeCurrentLibrary(refresh: Boolean): Flow<Library> = emptyFlow()
  override fun observeAllLibraries(refresh: Boolean): Flow<List<Library>> = flowOf(emptyList())
  override fun createLibraryItemPager(
    user: User,
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): LibraryItemPager = throw NotImplementedError("not exercised in these tests")

  override fun observeFilteredLibraryCount(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Int?> = flowOf(null)

  override suspend fun setCurrentLibrary(library: Library) = Unit

  override suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext> {
    addPodcastContextCalls += libraryId
    return addPodcastContextResult
  }
}
