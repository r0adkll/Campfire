// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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
