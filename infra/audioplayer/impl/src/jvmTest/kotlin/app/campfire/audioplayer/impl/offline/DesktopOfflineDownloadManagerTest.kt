// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownload.State
import app.campfire.audioplayer.test.fixtures.session
import app.campfire.audioplayer.test.fixtures.track
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.session.UserSession
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class DesktopOfflineDownloadManagerTest {

  private val userRoot: File = createTempDirectory("campfire-home").toFile()
  private val previousUserRoot: String? = System.setProperty("java.util.prefs.userRoot", userRoot.absolutePath)
  private val scope = CoroutineScope(Job())

  private val tracks = listOf(track(1, 0f, 60f))
  private val book = session(tracks = tracks).libraryItem
  private val episode = PodcastEpisode(
    id = "episode-1",
    libraryItemId = book.id,
    podcastId = book.media.id,
    title = "Episode One",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 60_000L,
    sizeInBytes = 0L,
    audioTrack = tracks.first(),
  )

  private val accountManager = FakeAccountManager()
  private val manager = DesktopOfflineDownloadManager(
    accountManager = accountManager,
    tokenRefresher = FakeTokenRefresher(accountManager),
    userSessionManager = FakeUserSessionManager(UserSession.LoggedIn(downloadingUser)),
    // The server refuses every download, as it does for a user without download permission
    client = HttpClient(MockEngine { respond("", HttpStatusCode.Forbidden) }),
    scope = scope,
  )

  @AfterTest
  fun tearDown() {
    scope.cancel()
    if (previousUserRoot == null) {
      System.clearProperty("java.util.prefs.userRoot")
    } else {
      System.setProperty("java.util.prefs.userRoot", previousUserRoot)
    }
    userRoot.deleteRecursively()
  }

  @Test
  fun `downloads land in the config directory and report through every observer`() = runTest {
    manager.downloadAll(listOf(book))
    val failed = manager.observeForItem(book).first { it.state == State.Failed }

    assertThat(failed.libraryItemId).isEqualTo(book.id)
    assertThat(File(userRoot, ".config/Campfire/downloads/item-1/item/manifest.json").isFile).isTrue()
    assertThat(manager.getForItem(book).state).isEqualTo(State.Failed)
    assertThat(manager.observeForItems(listOf(book)).first()[book.id]!!.state).isEqualTo(State.Failed)
    assertThat(manager.observeAll().first().map { it.libraryItemId }).containsExactly(book.id)
    assertThat(manager.localPathFor(book.id, null, tracks.first())).isNull()

    manager.stop(book)
    assertThat(manager.observeAll().first { it.isEmpty() }).isEmpty()
  }

  @Test
  fun `episode downloads forward to the queue`() = runTest {
    manager.downloadEpisode(book, episode)
    assertThat(manager.observeForEpisode(book, episode).first { it.state == State.Failed }.episodeId)
      .isEqualTo(episode.id)
    assertThat(manager.observeForEpisodes(book, listOf(episode)).first()[episode.id]!!.state)
      .isEqualTo(State.Failed)

    manager.stopEpisode(book, episode)
    assertThat(manager.observeForEpisode(book, episode).first().state).isEqualTo(State.None)

    manager.downloadEpisode(book, episode)
    manager.deleteEpisode(book, episode)
    manager.download(book)
    manager.delete(book)
    manager.downloadEpisode(book, episode)
    manager.deleteAllForItemId(book.id)
    manager.resumeDownloads()
    assertThat(manager.observeAll().first { it.isEmpty() }).isEmpty()
  }

  @Test
  fun `the initializer resumes downloads at startup`() = runTest {
    val downloads = FakeOfflineDownloadManager()

    DesktopOfflineDownloadInitializer(downloads).onInitialize()

    assertThat(downloads.invocations).containsExactly(FakeOfflineDownloadManager.Invocation.ResumeDownloads)
  }
}
