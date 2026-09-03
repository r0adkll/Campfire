// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.api

import app.campfire.core.model.LibraryStats
import app.campfire.core.model.ListeningStats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {

  fun getLibraryStats(): Flow<LibraryStats>
  fun getUserStats(): Flow<ListeningStats>

  /**
   * Force-refetch both the user listening stats and the current library's stats from
   * the server, updating any active [getUserStats]/[getLibraryStats] collectors with
   * the fresh data. Returns a failure instead of throwing so callers can keep showing
   * the cached data when the server is unreachable.
   */
  suspend fun refresh(): Result<Unit>
}
