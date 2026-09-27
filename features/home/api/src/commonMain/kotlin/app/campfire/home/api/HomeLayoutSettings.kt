// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.api

import app.campfire.core.model.LibraryId
import app.campfire.home.api.model.HomeLayoutEntry
import kotlinx.coroutines.flow.Flow

/**
 * The current user's customized home layouts, one per library, stored on this device only.
 */
interface HomeLayoutSettings {

  /**
   * Observe the saved layout for [libraryId], or null when the user hasn't customized it and
   * Home should follow the server's order.
   */
  fun observeLayout(libraryId: LibraryId): Flow<List<HomeLayoutEntry>?>

  fun setLayout(libraryId: LibraryId, entries: List<HomeLayoutEntry>)

  /** Drop the saved layout for [libraryId] so Home follows the server's order again. */
  fun resetLayout(libraryId: LibraryId)
}
