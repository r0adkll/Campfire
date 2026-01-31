package app.campfire.series.api

import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.series.api.paging.SeriesPager
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {

  fun observeAllSeries(): Flow<List<Series>>

  fun createSeriesPager(
    user: User,
    filter: ContentFilter? = null,
    sortMode: ContentSortMode = ContentSortMode.Name,
    sortDirection: SortDirection = SortDirection.Default,
  ): SeriesPager

  fun observeFilteredSeriesCount(
    filter: ContentFilter? = null,
    sortMode: ContentSortMode = ContentSortMode.Name,
    sortDirection: SortDirection = SortDirection.Default,
  ): Flow<Int?>

  fun observeSeriesLibraryItems(seriesId: String): Flow<List<LibraryItem>>
}
