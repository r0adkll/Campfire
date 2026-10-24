// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.filters

import app.campfire.core.model.FilterData
import app.campfire.core.model.LibraryId
import kotlinx.coroutines.flow.Flow

/**
 * An interface to pulling the information needed to fully populate the item filtering
 * UI.
 */
interface FilteringRepository {
  /** The filter data of the library the user has selected */
  fun observeFilterData(): Flow<FilterData>

  /** The filter data of [libraryId], such as every author, series and genre in it */
  fun observeFilterData(libraryId: LibraryId): Flow<FilterData>
}
