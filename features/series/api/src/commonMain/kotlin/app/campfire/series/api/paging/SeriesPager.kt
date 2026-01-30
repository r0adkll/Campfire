package app.campfire.series.api.paging

import androidx.paging.Pager
import app.campfire.core.model.Series
import kotlinx.coroutines.flow.Flow

class SeriesPager(
  val pager: Pager<Int, Series>,
  val countFlow: Flow<Int?>,
)
