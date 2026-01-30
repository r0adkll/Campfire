package app.campfire.series.test

import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.series.api.SeriesRepository
import app.campfire.series.api.paging.SeriesPager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeSeriesRepository : SeriesRepository {

  val allSeriesFlow = MutableSharedFlow<List<Series>>(replay = 1)
  override fun observeAllSeries(): Flow<List<Series>> {
    return allSeriesFlow
  }

  val seriesLibraryItemsFlow = MutableSharedFlow<List<LibraryItem>>(replay = 1)
  override fun observeSeriesLibraryItems(seriesId: String): Flow<List<LibraryItem>> {
    return seriesLibraryItemsFlow
  }

  override fun observeSeriesPager(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<SeriesPager> {
    TODO("Not yet implemented")
  }
}
