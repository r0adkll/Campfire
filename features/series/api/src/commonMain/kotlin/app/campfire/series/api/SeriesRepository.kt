package app.campfire.series.api

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {

  fun observeAllSeries(): Flow<List<Series>>
  fun observeSeriesLibraryItems(seriesId: String): Flow<List<LibraryItem>>
}
