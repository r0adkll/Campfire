// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.downloads

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItemId
import app.campfire.network.models.PodcastEpisodeDownload
import app.campfire.podcasts.api.EpisodeDownloadsSnapshot
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.RemoteEpisodeDownloadTracker
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

private val FINISHED_TTL = 60.seconds

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class, boundType = RemoteEpisodeDownloadTracker::class)
@ContributesBinding(UserScope::class, boundType = RemoteEpisodeDownloadSink::class)
@Inject
class DefaultRemoteEpisodeDownloadTracker(
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : RemoteEpisodeDownloadTracker,
  RemoteEpisodeDownloadSink {

  private val _state = MutableStateFlow<Map<LibraryItemId, List<RemoteEpisodeDownload>>>(emptyMap())
  override val state: StateFlow<Map<LibraryItemId, List<RemoteEpisodeDownload>>> = _state.asStateFlow()

  private val _recentlyFinishedUrls = MutableStateFlow<Set<String>>(emptySet())
  override val recentlyFinishedUrls: StateFlow<Set<String>> = _recentlyFinishedUrls.asStateFlow()

  override fun observe(libraryItemId: LibraryItemId): Flow<List<RemoteEpisodeDownload>> {
    return state
      .map { it[libraryItemId].orEmpty() }
      .distinctUntilChanged()
  }

  override fun onQueued(download: PodcastEpisodeDownload) {
    upsert(download, RemoteEpisodeDownload.State.Queued)
  }

  override fun onStarted(download: PodcastEpisodeDownload) {
    upsert(download, RemoteEpisodeDownload.State.Downloading)
  }

  override fun onFinished(download: PodcastEpisodeDownload) {
    _state.update { current ->
      val list = current[download.libraryItemId].orEmpty()
      val updated = list.filter { it.id != download.id }
      if (updated.isEmpty()) {
        current - download.libraryItemId
      } else {
        current + (download.libraryItemId to updated)
      }
    }
    // Only successful finishes get a transient "just finished" entry — failures fall straight
    // back to Available so the row doesn't misleadingly show as downloaded.
    if (!download.failed) {
      val url = download.url
      _recentlyFinishedUrls.update { it + url }
      coroutineScopeHolder.get().launch {
        delay(FINISHED_TTL)
        _recentlyFinishedUrls.update { it - url }
      }
    }
  }

  override fun onQueueCleared(libraryItemId: LibraryItemId) {
    _state.update { current ->
      val list = current[libraryItemId].orEmpty()
      // Server keeps the actively-downloading item; only queued items are dropped.
      val kept = list.filter { it.state != RemoteEpisodeDownload.State.Queued }
      if (kept.isEmpty()) {
        current - libraryItemId
      } else {
        current + (libraryItemId to kept)
      }
    }
  }

  override fun applySnapshot(libraryId: LibraryId, snapshot: EpisodeDownloadsSnapshot) {
    val fresh = buildList {
      snapshot.currentDownload?.let { add(it) }
      addAll(snapshot.queue)
    }
    _state.update { current ->
      // Drop any existing entries that belong to this library so the fresh snapshot wins.
      val pruned = current.mapValues { (_, list) -> list.filter { it.libraryId != libraryId } }
        .filterValues { it.isNotEmpty() }
      val byLibraryItem = fresh.groupBy { it.libraryItemId }
      pruned + byLibraryItem
    }
  }

  private fun upsert(download: PodcastEpisodeDownload, state: RemoteEpisodeDownload.State) {
    _state.update { current ->
      val list = current[download.libraryItemId].orEmpty()
      val withoutThis = list.filter { it.id != download.id }
      current + (download.libraryItemId to (withoutThis + download.toDomain(state)))
    }
  }
}
