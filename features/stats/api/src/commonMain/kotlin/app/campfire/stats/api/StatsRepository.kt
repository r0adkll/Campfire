// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.api

import app.campfire.core.model.LibraryStats
import app.campfire.core.model.ListeningStats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {

  fun getLibraryStats(): Flow<LibraryStats>
  fun getUserStats(): Flow<ListeningStats>
}
