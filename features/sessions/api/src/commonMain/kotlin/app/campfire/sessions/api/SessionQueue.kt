// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.api

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

interface SessionQueue {

  suspend fun add(libraryItem: LibraryItem, episode: PodcastEpisode? = null)
  suspend fun addAll(libraryItems: List<LibraryItem>)

  /**
   * Puts the entry at the front of the queue so it plays next, moving it there if it is
   * already queued.
   */
  suspend fun addFirst(libraryItem: LibraryItem, episode: PodcastEpisode? = null)
  suspend fun remove(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId? = null)
  suspend fun pop(): QueuedEntry?
  suspend fun reorder(entries: List<QueuedEntry>)
  suspend fun clear()

  fun observeAll(): Flow<List<QueuedEntry>>
}

@OptIn(ExperimentalCoroutinesApi::class)
fun SessionQueue.observeContains(
  libraryItemId: LibraryItemId,
  episodeId: PodcastEpisodeId? = null,
): Flow<Boolean> {
  return observeAll()
    .mapLatest { list ->
      list.any { it.libraryItemId == libraryItemId && it.episodeId == episodeId }
    }
}
