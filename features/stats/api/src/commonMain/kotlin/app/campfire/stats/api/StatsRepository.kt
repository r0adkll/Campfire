package app.campfire.stats.api

import app.campfire.core.model.LibraryStats
import app.campfire.core.model.ListeningStats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {

  fun getLibraryStats(): Flow<LibraryStats>
  fun getUserStats(): Flow<ListeningStats>
}
