package app.campfire.series.api

import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.series.api.paging.SeriesPager
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {

  fun observeAllSeries(): Flow<List<Series>>

  fun observeSeriesPager(
    filter: ContentFilter? = null,
    sortMode: ContentSortMode = ContentSortMode.Name,
    sortDirection: SortDirection = SortDirection.Default,
  ): Flow<SeriesPager>

  fun observeSeriesLibraryItems(seriesId: String): Flow<List<LibraryItem>>
}
