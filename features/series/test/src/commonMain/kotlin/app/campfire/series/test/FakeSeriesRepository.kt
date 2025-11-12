package app.campfire.series.test

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.series.api.SeriesRepository
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
}
