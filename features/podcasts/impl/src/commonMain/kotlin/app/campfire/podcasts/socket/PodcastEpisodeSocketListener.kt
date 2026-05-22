package app.campfire.podcasts.socket

import app.campfire.core.di.UserScope
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.podcasts.downloads.RemoteEpisodeDownloadSink
import app.campfire.socket.events.EpisodeAdded
import app.campfire.socket.events.EpisodeDownloadFinished
import app.campfire.socket.events.EpisodeDownloadQueueCleared
import app.campfire.socket.events.EpisodeDownloadQueued
import app.campfire.socket.events.EpisodeDownloadStarted
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

/**
 * Reacts to podcast/episode socket events from the Audiobookshelf server.
 *
 * - [EpisodeAdded] lands the new episode in the local DB via the parent `LibraryItemExpanded.Podcast`;
 *   `LibraryItemSourceOfTruth` then propagates to any open detail screens.
 * - The four `EpisodeDownload*` events feed the in-memory `RemoteEpisodeDownloadTracker` via its
 *   package-internal sink, mirroring the server's queue state for the "Downloads" feature.
 */
@ContributesMultibinding(UserScope::class, boundType = SocketEventListener::class)
@Inject
class PodcastEpisodeSocketListener(
  private val libraryItemDao: LibraryItemDao,
  private val downloadSink: RemoteEpisodeDownloadSink,
) : SocketEventListener {
  override suspend fun handle(event: SocketEvent) {
    when (event) {
      is EpisodeAdded -> libraryItemDao.insert(
        item = event.libraryItem,
        asTransaction = true,
        ignoreOnInsert = true,
      )
      is EpisodeDownloadQueued -> downloadSink.onQueued(event.download)
      is EpisodeDownloadStarted -> downloadSink.onStarted(event.download)
      is EpisodeDownloadFinished -> downloadSink.onFinished(event.download)
      is EpisodeDownloadQueueCleared -> downloadSink.onQueueCleared(event.libraryItemId)
      else -> Unit
    }
  }
}
