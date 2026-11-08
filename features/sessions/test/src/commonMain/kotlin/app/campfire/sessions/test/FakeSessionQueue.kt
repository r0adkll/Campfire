// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.test

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.preview.libraryItem
import app.campfire.sessions.api.QueuedEntry
import app.campfire.sessions.api.SessionQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription

class FakeSessionQueue : SessionQueue {
  val queue = ArrayDeque<QueuedEntry>()
  private val queueFlow = MutableSharedFlow<List<QueuedEntry>>()

  override suspend fun add(libraryItem: LibraryItem, episode: PodcastEpisode?) {
    val entry = QueuedEntry(libraryItem, episode)
    if (queue.none { it.key == entry.key }) queue.addLast(entry)
    emit()
  }

  /** Queues stand-ins built from the ids, as the real queue hydrates them from the database. */
  override suspend fun add(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    add(
      libraryItem = libraryItem(id = libraryItemId),
      episode = episodeId?.let { stubEpisode(libraryItemId, it) },
    )
  }

  private fun stubEpisode(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId) = PodcastEpisode(
    id = episodeId,
    libraryItemId = libraryItemId,
    podcastId = libraryItemId,
    title = episodeId,
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = 0L,
  )

  override suspend fun addAll(libraryItems: List<LibraryItem>) {
    queue.addAll(libraryItems.map { QueuedEntry(it) })
    emit()
  }

  override suspend fun addFirst(libraryItem: LibraryItem, episode: PodcastEpisode?) {
    val entry = QueuedEntry(libraryItem, episode)
    queue.removeAll { it.key == entry.key }
    queue.addFirst(entry)
    emit()
  }

  override suspend fun insert(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?, index: Int) {
    val existing = queue.firstOrNull { it.libraryItemId == libraryItemId && it.episodeId == episodeId }
    if (existing != null) queue.remove(existing)
    val entry = existing
      ?: QueuedEntry(libraryItem(id = libraryItemId), episodeId?.let { stubEpisode(libraryItemId, it) })
    queue.add(index.coerceIn(0, queue.size), entry)
    emit()
  }

  override suspend fun remove(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?): Int? {
    val index = queue.indexOfFirst { it.libraryItemId == libraryItemId && it.episodeId == episodeId }
    if (index >= 0) queue.removeAt(index)
    emit()
    return index.takeIf { it >= 0 }
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
