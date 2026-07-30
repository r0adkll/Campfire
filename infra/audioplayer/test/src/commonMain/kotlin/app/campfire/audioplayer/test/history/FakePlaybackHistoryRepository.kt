// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.test.history

import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart

class FakePlaybackHistoryRepository : PlaybackHistoryRepository {

  // Keyed by (libraryItemId, episodeId?). Episode-scoped histories live under their own
  // entries so a clear on one episode doesn't wipe siblings.
  val history = mutableMapOf<HistoryKey, List<PlaybackAction>>()

  val updatesFlow = MutableSharedFlow<Unit>()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observe(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): Flow<List<PlaybackAction>> {
    val key = HistoryKey(libraryItemId, episodeId)
    return updatesFlow
      .mapLatest {
        history[key] ?: emptyList()
      }
      .onStart {
        history[key]?.let {
          emit(it)
        }
      }
  }

  override suspend fun get(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): List<PlaybackAction> {
    return history[HistoryKey(libraryItemId, episodeId)] ?: emptyList()
  }

  override suspend fun clear(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    if (episodeId != null) {
      history[HistoryKey(libraryItemId, episodeId)] = emptyList()
    } else {
      // Item-level clear wipes every keyed entry for this libraryItem.
      history.keys
        .filter { it.libraryItemId == libraryItemId }
        .forEach { history[it] = emptyList() }
    }
    updatesFlow.emit(Unit)
  }

  override suspend fun clearAll() {
    history.clear()
    updatesFlow.emit(Unit)
  }

  data class HistoryKey(
    val libraryItemId: LibraryItemId,
    val episodeId: PodcastEpisodeId? = null,
  )
}
