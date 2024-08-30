package app.campfire.series.api

import app.campfire.core.model.Series
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {

  fun observeAllSeries(): Flow<List<Series>>
}
