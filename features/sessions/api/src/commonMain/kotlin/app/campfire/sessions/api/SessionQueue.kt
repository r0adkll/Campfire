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
