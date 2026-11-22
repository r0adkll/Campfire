// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import app.campfire.audioplayer.QueueOnStop
import app.campfire.audioplayer.impl.engine.DesktopAudioEngineProvider
import app.campfire.audioplayer.impl.engine.FakePlaybackEngine
import app.campfire.audioplayer.impl.engine.PlaybackEngine
import app.campfire.audioplayer.impl.offline.DesktopOfflineDownloadManager
import app.campfire.audioplayer.impl.offline.FakeAccountManager
import app.campfire.audioplayer.impl.offline.FakeTokenRefresher
import app.campfire.audioplayer.impl.offline.FakeUserSessionManager
import app.campfire.audioplayer.impl.offline.downloadingUser
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.audioplayer.impl.sleep.FakeSleepTimerManager
import app.campfire.audioplayer.test.FakeAudioOutputController
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.audioplayer.test.fixtures.session
import app.campfire.audioplayer.test.fixtures.track
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Session
import app.campfire.core.session.UserSession
import app.campfire.settings.test.FakeEqualizerSettings
import app.campfire.settings.test.FakePlaybackSettings
import assertk.assertThat
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

class DesktopPlaybackControllerTest {

  private val userRoot: File = createTempDirectory("campfire-home").toFile()
  private val previousUserRoot: String? = System.setProperty("java.util.prefs.userRoot", userRoot.absolutePath)
  private val downloadScope = CoroutineScope(Job())
  private val userScopeJob = SupervisorJob()

  private val holder = FakeAudioPlayerHolder()
  private val sessionManager = QueueingSessionManager()
  private val accountManager = FakeAccountManager()
  private val userSessionManager = FakeUserSessionManager(UserSession.LoggedIn(downloadingUser))

  private val controller = DesktopPlaybackController(
    playbackSessionManager = sessionManager,
    playbackSettings = FakePlaybackSettings(),
    equalizerSettings = FakeEqualizerSettings(),
    audioPlayerHolder = holder,
    sleepTimerManagerFactory = FakeSleepTimerManager().factory,
    accountManager = accountManager,
    serverRequestHeaders = ServerRequestHeaders(accountManager, userSessionManager),
    audioOutputController = FakeAudioOutputController(),
    engineProviders = setOf(FakeEngineProvider()),
    offlineDownloadManager = DesktopOfflineDownloadManager(
      accountManager = accountManager,
      tokenRefresher = FakeTokenRefresher(accountManager),
      userSessionManager = userSessionManager,
      client = HttpClient(MockEngine { respond("", HttpStatusCode.NotFound) }),
      scope = downloadScope,
    ),
    userScopeHolder = CoroutineScopeHolder { CoroutineScope(userScopeJob + Dispatchers.Default) },
  )

  private val tracks = listOf(track(1, 0f, 60f))
  private val playing = session(tracks = tracks)
  private val queued = session(tracks = tracks)

  @AfterTest
  fun tearDown() {
    holder.release()
    downloadScope.cancel()
    userScopeJob.cancel()
    if (previousUserRoot == null) {
      System.clearProperty("java.util.prefs.userRoot")
    } else {
      System.setProperty("java.util.prefs.userRoot", previousUserRoot)
    }
    userRoot.deleteRecursively()
  }

  @Test
  fun `stopping with play next keeps the player the queued item started on`() = runBlocking {
    startPlaying()
    sessionManager.next = queued

    controller.stopSession(playing.libraryItem.id, QueueOnStop.PlayNext)
    awaitControllerWork()

    assertThat(holder.currentPlayer.value?.preparedSession).isSameInstanceAs(queued)
  }

  @Test
  fun `stopping with play next and an empty queue releases the player`() = runBlocking {
    startPlaying()

    controller.stopSession(playing.libraryItem.id, QueueOnStop.PlayNext)
    awaitControllerWork()

    assertThat(holder.currentPlayer.value).isNull()
  }

  @Test
  fun `stopping without play next releases the player`() = runBlocking {
    startPlaying()
    sessionManager.next = queued

    controller.stopSession(playing.libraryItem.id, QueueOnStop.Keep)
    awaitControllerWork()

    assertThat(holder.currentPlayer.value).isNull()
  }

  private suspend fun startPlaying() {
    sessionManager.toStart = playing
    controller.startSession(playing.libraryItem.id)
    awaitControllerWork()
    assertThat(holder.currentPlayer.value?.preparedSession).isSameInstanceAs(playing)
  }

  private suspend fun awaitControllerWork() {
    userScopeJob.children.forEach { it.join() }
  }

  /** Prepares the player the way [DefaultPlaybackSessionManager][app.campfire.audioplayer.impl.session.DefaultPlaybackSessionManager] does. */
  private inner class QueueingSessionManager : PlaybackSessionManager {
    var toStart: Session? = null
    var next: Session? = null

    override suspend fun startSession(
      libraryItemId: LibraryItemId,
      playImmediately: Boolean,
      chapterId: Int?,
      episodeId: PodcastEpisodeId?,
      methodOverride: PlayMethod?,
      requeueCurrent: Boolean,
    ) {
      toStart?.let { holder.currentPlayer.value?.prepare(it, playImmediately, chapterId) {} }
    }

    override suspend fun stopSession(libraryItemId: LibraryItemId, queue: QueueOnStop, episodeId: PodcastEpisodeId?) {
      val next = next
      if (queue == QueueOnStop.PlayNext && next != null) {
        holder.currentPlayer.value?.prepare(next, true, null) {}
      }
    }
  }

  private class FakeEngineProvider : DesktopAudioEngineProvider {
    override val name: String = "fake"
    override val factory: PlaybackEngine.Factory = PlaybackEngine.Factory { FakePlaybackEngine() }
    override val supportsHls: Boolean = false
  }
}
