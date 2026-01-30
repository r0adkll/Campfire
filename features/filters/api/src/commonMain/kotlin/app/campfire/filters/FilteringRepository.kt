package app.campfire.filters

import app.campfire.core.model.FilterData
import kotlinx.coroutines.flow.Flow

/**
 * An interface to pulling the information needed to fully populate the item filtering
 * UI.
 */
interface FilteringRepository {
  fun observeFilterData(): Flow<FilterData>
}
