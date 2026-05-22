package app.campfire.podcasts.ui.downloads

import app.campfire.core.model.LibraryItemId
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.RemoteEpisodeDownloadTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeRemoteEpisodeDownloadTracker(
  initial: Map<LibraryItemId, List<RemoteEpisodeDownload>> = emptyMap(),
  initialFinishedUrls: Set<String> = emptySet(),
) : RemoteEpisodeDownloadTracker {
  private val mutableState = MutableStateFlow(initial)
  override val state: StateFlow<Map<LibraryItemId, List<RemoteEpisodeDownload>>> = mutableState

  private val mutableFinishedUrls = MutableStateFlow(initialFinishedUrls)
  override val recentlyFinishedUrls: StateFlow<Set<String>> = mutableFinishedUrls

  override fun observe(libraryItemId: LibraryItemId): Flow<List<RemoteEpisodeDownload>> {
    return state.map { it[libraryItemId].orEmpty() }.distinctUntilChanged()
  }

  fun setState(value: Map<LibraryItemId, List<RemoteEpisodeDownload>>) {
    mutableState.value = value
  }

  fun setRecentlyFinishedUrls(urls: Set<String>) {
    mutableFinishedUrls.value = urls
  }
}
