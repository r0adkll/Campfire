// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions

import app.campfire.common.test.session
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Session
import app.campfire.core.model.UserId
import app.campfire.core.time.GrandFatherTime
import app.campfire.home.ui.libraryItem
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import app.campfire.sessions.db.SessionDataSource
import app.campfire.sessions.sync.ServerSessionAttacher
import app.campfire.sessions.test.FakeStreamingRoutePredictor
import app.campfire.user.test.FakeMediaProgressRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class DefaultSessionsRepositoryTest {

  private val downloads = FakeDownloads()
  private val dataSource = FakeSessionDataSource()
  private val attacher = FakeServerSessionAttacher()
  private val routePredictor = FakeStreamingRoutePredictor()

  private val repository = DefaultSessionsRepository(
    fatherTime = GrandFatherTime,
    mediaProgressRepository = FakeMediaProgressRepository(),
    downloads = downloads,
    dataSource = dataSource,
    serverSessionAttacher = attacher,
    streamingRoutePredictor = routePredictor,
  )

  @Test
  fun createSession_playsADownloadedBookFromTheDevice() = runTest {
    downloads.downloads.value = listOf(Download(DownloadContent.Audiobook(ITEM), Download.State.Downloaded))
    routePredictor.wouldStreamHlsFlow.value = true

    val session = repository.createSession(ITEM)

    assertThat(session.playMethod).isEqualTo(PlayMethod.Local)
    assertThat(attacher.opened).isEqualTo(0)
    assertThat(attacher.attached).containsExactly(session)
  }

  @Test
  fun createSession_streamsABookWhoseDownloadIsntFinished() = runTest {
    for (state in Download.State.entries - Download.State.Downloaded) {
      downloads.downloads.value = listOf(Download(DownloadContent.Audiobook(ITEM), state))

      val session = repository.createSession(ITEM)

      assertThat(session.playMethod, name = "$state").isEqualTo(PlayMethod.DirectPlay)
      dataSource.sessions.clear()
    }
  }

  @Test
  fun createSession_streamsADownloadedEpisode() = runTest {
    downloads.downloads.value = listOf(
      Download(DownloadContent.Audiobook(ITEM), Download.State.Downloaded),
      Download(DownloadContent.Episode(ITEM, EPISODE), Download.State.Downloaded),
    )

    val session = repository.createSession(ITEM, EPISODE)

    assertThat(session.playMethod).isEqualTo(PlayMethod.DirectPlay)
    assertThat(session.episodeId).isEqualTo(EPISODE)
  }

  @Test
  fun createSession_opensAnHlsStream_whenTheRouteWantsOne() = runTest {
    routePredictor.wouldStreamHlsFlow.value = true
    attacher.transcode = ServerSessionAttacher.TranscodeStream("server_1", "/hls/server_1/output.m3u8")

    val session = repository.createSession(ITEM)

    assertThat(session.playMethod).isEqualTo(PlayMethod.Transcode)
    assertThat(session.serverSessionId).isEqualTo("server_1")
    assertThat(session.hlsStreamUrl).isEqualTo("/hls/server_1/output.m3u8")
  }

  @Test
  fun createSession_directPlays_whenTheHlsStreamWontOpen() = runTest {
    routePredictor.wouldStreamHlsFlow.value = true

    val session = repository.createSession(ITEM)

    assertThat(attacher.opened).isEqualTo(1)
    assertThat(session.playMethod).isEqualTo(PlayMethod.DirectPlay)
  }

  @Test
  fun createSession_keepsAnHlsListenOnHls_reusingItsStream() = runTest {
    routePredictor.canStreamHls = true
    dataSource.sessions[ITEM] = session(libraryItem = libraryItem(id = ITEM), playMethod = PlayMethod.Transcode)
      .copy(serverSessionId = "server_1", hlsStreamUrl = "/hls/server_1/output.m3u8")

    val session = repository.createSession(ITEM)

    assertThat(session.playMethod).isEqualTo(PlayMethod.Transcode)
    assertThat(session.serverSessionId).isEqualTo("server_1")
    assertThat(attacher.opened).isEqualTo(0)
  }

  @Test
  fun createSession_leavesHls_whenTheListenerPicksDirectPlay() = runTest {
    dataSource.sessions[ITEM] = session(libraryItem = libraryItem(id = ITEM), playMethod = PlayMethod.Transcode)
      .copy(serverSessionId = "server_1", hlsStreamUrl = "/hls/server_1/output.m3u8")

    val session = repository.createSession(ITEM, methodOverride = PlayMethod.DirectPlay)

    assertThat(session.playMethod).isEqualTo(PlayMethod.DirectPlay)
    assertThat(session.serverSessionId).isNull()
    assertThat(session.hlsStreamUrl).isNull()
  }

  @Test
  fun sessionChanges_goToTheDataSource() = runTest {
    val created = repository.createSession(ITEM)

    assertThat(repository.getSession(ITEM)).isEqualTo(created)
    assertThat(repository.getCurrentSession()).isEqualTo(created)
    assertThat(repository.observeCurrentSession().first()).isEqualTo(created)

    repository.updateCurrentTime(ITEM, 30.seconds)
    repository.updateLastPlayed(ITEM)
    repository.addTimeListening(ITEM, 5.seconds)
    repository.stopSession(ITEM, null)
    repository.markFinished(ITEM, null)
    repository.markDeleted(ITEM, null)
    repository.deleteSession(ITEM, null)

    assertThat(dataSource.calls).containsExactly(
      "updateCurrentTime $ITEM 30s",
      "updateLastPlayed $ITEM",
      "addTimeListening $ITEM 5s",
      "stopSession $ITEM null",
      "markFinished $ITEM null",
      "markDeleted $ITEM null",
      "deleteSession $ITEM null",
    )
  }

  private companion object {
    const val ITEM: LibraryItemId = "li_1"
    const val EPISODE: PodcastEpisodeId = "ep_1"
  }
}

/** One row per item, reused while its episode matches, as the real table is. */
private class FakeSessionDataSource : SessionDataSource {

  val sessions = mutableMapOf<LibraryItemId, Session>()
  val calls = mutableListOf<String>()
  private val current = MutableStateFlow<Session?>(null)

  override fun observeCurrentSession(): Flow<Session?> = current
  override suspend fun getCurrentSession(): Session? = current.value
  override suspend fun getSession(libraryItemId: LibraryItemId): Session? = sessions[libraryItemId]
  override suspend fun getSessions(userId: UserId): List<Session> = sessions.values.toList()

  override suspend fun createOrStartSession(
    libraryItemId: LibraryItemId,
    playMethod: PlayMethod,
    progress: MediaProgress?,
    episodeId: PodcastEpisodeId?,
  ): Session {
    val session = sessions[libraryItemId]?.takeIf { !it.isDeleted && it.episodeId == episodeId }
      ?: session(libraryItem = libraryItem(id = libraryItemId), playMethod = playMethod).copy(episodeId = episodeId)
    sessions[libraryItemId] = session
    current.value = session
    return session
  }

  override suspend fun attachServerSession(
    libraryItemId: LibraryItemId,
    serverSessionId: String,
    episodeId: PodcastEpisodeId?,
    hlsStreamPath: String?,
  ) = update(libraryItemId) { it.copy(serverSessionId = serverSessionId, hlsStreamUrl = hlsStreamPath) }

  override suspend fun clearServerSession(libraryItemId: LibraryItemId) =
    update(libraryItemId) { it.copy(serverSessionId = null, hlsStreamUrl = null) }

  override suspend fun updatePlayMethod(libraryItemId: LibraryItemId, playMethod: PlayMethod) =
    update(libraryItemId) { it.copy(playMethod = playMethod) }

  override suspend fun updateReportedTimeListening(libraryItemId: LibraryItemId, reported: Duration) =
    update(libraryItemId) { it.copy(reportedTimeListening = reported) }

  override suspend fun updateCurrentTime(libraryItemId: LibraryItemId, currentTime: Duration) {
    calls += "updateCurrentTime $libraryItemId $currentTime"
  }

  override suspend fun updateLastPlayed(libraryItemId: LibraryItemId) {
    calls += "updateLastPlayed $libraryItemId"
  }

  override suspend fun addTimeListening(libraryItemId: LibraryItemId, amount: Duration) {
    calls += "addTimeListening $libraryItemId $amount"
  }

  override suspend fun markDeleted(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    calls += "markDeleted $libraryItemId $episodeId"
  }

  override suspend fun deleteSession(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    calls += "deleteSession $libraryItemId $episodeId"
  }

  override suspend fun stopSession(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    calls += "stopSession $libraryItemId $episodeId"
  }

  override suspend fun markFinished(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId?) {
    calls += "markFinished $libraryItemId $episodeId"
  }

  private fun update(libraryItemId: LibraryItemId, change: (Session) -> Session) {
    sessions[libraryItemId] = change(sessions.getValue(libraryItemId))
  }
}

private class FakeServerSessionAttacher : ServerSessionAttacher {

  var transcode: ServerSessionAttacher.TranscodeStream? = null
  var opened = 0
  val attached = mutableListOf<Session>()

  override fun isAttachPending(libraryItemId: LibraryItemId): Boolean = false

  override fun attachAsync(session: Session) {
    attached += session
  }

  override suspend fun openTranscodeSession(session: Session): ServerSessionAttacher.TranscodeStream? {
    opened++
    return transcode
  }
}
