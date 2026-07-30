// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.test

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.sessions.api.QueuedEntry
import app.campfire.sessions.api.SessionQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription

class FakeSessionQueue : SessionQueue {
  val queue = ArrayDeque<QueuedEntry>()
  private val queueFlow = MutableSharedFlow<List<QueuedEntry>>()

  override suspend fun add(libraryItem: LibraryItem, episode: PodcastEpisode?) {
    queue.addLast(QueuedEntry(libraryItem, episode))
    emit()
  }

  override suspend fun addAll(libraryItems: List<LibraryItem>) {
    queue.addAll(libraryItems.map { QueuedEntry(it) })
    emit()
  }

  override suspend fun remove(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    queue.removeAll { it.libraryItemId == libraryItemId && it.episodeId == episodeId }
    emit()
  }

  override suspend fun pop(): QueuedEntry? {
    return queue.removeFirstOrNull().also {
      emit()
    }
  }

  override suspend fun reorder(entries: List<QueuedEntry>) {
    queue.clear()
    queue.addAll(entries)
    emit()
  }

  override suspend fun clear() {
    queue.clear()
    emit()
  }

  override fun observeAll(): Flow<List<QueuedEntry>> {
    return queueFlow
      .onSubscription {
        emit(queue)
      }
  }

  private suspend fun emit() {
    queueFlow.emit(queue)
  }
}
