// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions

import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Session
import app.campfire.core.time.FatherTime
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.sessions.api.SessionsRepository
import app.campfire.sessions.api.StreamingRoutePredictor
import app.campfire.sessions.db.SessionDataSource
import app.campfire.sessions.sync.ServerSessionAttacher
import app.campfire.user.api.MediaProgressRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultSessionsRepository(
  private val fatherTime: FatherTime,
  private val libraryItemRepository: LibraryItemRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val dataSource: SessionDataSource,
  private val serverSessionAttacher: ServerSessionAttacher,
  private val streamingRoutePredictor: StreamingRoutePredictor,
) : SessionsRepository {

  override fun observeCurrentSession(): Flow<Session?> {
    return dataSource.observeCurrentSession()
  }

  override suspend fun getCurrentSession(): Session? {
    return dataSource.getCurrentSession()
  }

  override suspend fun getSession(libraryItemId: LibraryItemId): Session? {
    return dataSource.getSession(libraryItemId)
  }

  override suspend fun createSession(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ): Session {
    val libraryItem = libraryItemRepository.getLibraryItem(libraryItemId)
    // Podcast sessions resume against the episode's progress row; books read item-level.
    val progress = mediaProgressRepository.getProgress(libraryItemId, episodeId)
    val offlineDownload = offlineDownloadManager.getForItem(libraryItem)

    val session = dataSource.createOrStartSession(
      libraryItemId = libraryItemId,
      playMethod = if (offlineDownload.isCompleted) {
        PlayMethod.Local
      } else {
        PlayMethod.DirectPlay
      },
      progress = progress,
      episodeId = episodeId,
    )

    val routed = routeStreamingMethod(session)

    // Opportunistic, fire-and-forget: playback never waits on this. If it lands, the row
    // reports through the server session; if not, nothing changes. No-ops for sessions the
    // router already attached (HLS) and for downloads.
    serverSessionAttacher.attachAsync(routed)

    return routed
  }

  /**
   * Decides how a streamed session is delivered, per [StreamingRoutePredictor] (the shared
   * decision the item detail UI also reflects): an HLS transcode session (the only path
   * that waits on the network, bounded — the playlist URL can't exist without the server
   * session id) or plain direct play. Every failure falls back to direct play, i.e. the
   * previously shipped behavior.
   */
  private suspend fun routeStreamingMethod(session: Session): Session {
    // Downloads always play locally
    if (session.playMethod == PlayMethod.Local) return session

    val wantsHls = streamingRoutePredictor.wouldStreamHls(session.libraryItem, session.episodeId)

    if (!wantsHls) {
      // A reused row can carry a previous HLS decision the setting no longer wants
      if (session.playMethod == PlayMethod.Transcode) {
        dataSource.clearServerSession(session.libraryItem.id)
        dataSource.updatePlayMethod(session.libraryItem.id, PlayMethod.DirectPlay)
        return dataSource.getSession(session.libraryItem.id) ?: session
      }
      return session
    }

    // Always open a fresh transcode session, even on a reused Transcode row: playlists die
    // with server restarts, and the server's per-device dedupe closes the previous one.
    val transcodeSessionId = serverSessionAttacher.openTranscodeSession(session) ?: return session
    dataSource.attachServerSession(session.libraryItem.id, transcodeSessionId, session.episodeId)
    dataSource.updatePlayMethod(session.libraryItem.id, PlayMethod.Transcode)
    return dataSource.getSession(session.libraryItem.id) ?: session
  }

  override suspend fun markDeleted(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    dataSource.markDeleted(libraryItemId, episodeId)
  }

  override suspend fun deleteSession(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    dataSource.deleteSession(libraryItemId, episodeId)
  }

  override suspend fun updateCurrentTime(libraryItemId: LibraryItemId, currentTime: Duration) {
    dataSource.updateCurrentTime(
      libraryItemId = libraryItemId,
      currentTime = currentTime,
    )
  }

  override suspend fun updateLastPlayed(libraryItemId: LibraryItemId) {
    dataSource.updateLastPlayed(
      libraryItemId = libraryItemId,
    )
  }

  override suspend fun addTimeListening(libraryItemId: LibraryItemId, amount: Duration) {
    dataSource.addTimeListening(
      libraryItemId = libraryItemId,
      amount = amount,
    )
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    dataSource.stopSession(libraryItemId, episodeId)
  }

  override suspend fun markFinished(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    dataSource.markFinished(libraryItemId, episodeId)
  }
}
