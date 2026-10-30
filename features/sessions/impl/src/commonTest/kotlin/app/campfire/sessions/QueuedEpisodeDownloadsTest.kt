// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.MediaType
import app.campfire.core.model.PodcastEpisode
import app.campfire.home.ui.libraryItem
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import app.campfire.offline.test.FakeDownloads.Invocation
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.settings.test.FakeDownloadSettings
import app.campfire.user.test.FakeUserRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class QueuedEpisodeDownloadsTest {

  private val sessionQueue = FakeSessionQueue()
  private val downloads = FakeDownloads()
  private val downloadSettings = FakeDownloadSettings().apply { downloadQueuedEpisodes = true }
  private val userRepository = FakeUserRepository()

  private val podcast = libraryItem(id = "podcast", mediaType = MediaType.Podcast)
  private val book = libraryItem(id = "book")

  @Test
  fun `an episode added to the queue is downloaded`() = downloadsTest {
    sessionQueue.add(podcast, episode("e1"))

    assertThat(downloads.invocations).containsExactly(
      Invocation.Download(listOf(DownloadContent.Episode(podcast.id, "e1"))),
    )
  }

  @Test
  fun `an episode moved to the front of the queue is downloaded`() = downloadsTest {
    sessionQueue.addFirst(podcast, episode("e1"))

    assertThat(downloads.invocations).containsExactly(
      Invocation.Download(listOf(DownloadContent.Episode(podcast.id, "e1"))),
    )
  }

  @Test
  fun `episodes already queued are left alone`() = runTest(UnconfinedTestDispatcher()) {
    sessionQueue.add(podcast, episode("e1"))
    start()

    sessionQueue.add(podcast, episode("e2"))
    sessionQueue.reorder(sessionQueue.queue.reversed())

    assertThat(downloads.invocations).containsExactly(
      Invocation.Download(listOf(DownloadContent.Episode(podcast.id, "e2"))),
    )
  }

  @Test
  fun `a queued book is not downloaded`() = downloadsTest {
    sessionQueue.add(book)

    assertThat(downloads.invocations).isEmpty()
  }

  @Test
  fun `nothing is downloaded while the setting is off`() = downloadsTest {
    downloadSettings.downloadQueuedEpisodes = false

    sessionQueue.add(podcast, episode("e1"))

    assertThat(downloads.invocations).isEmpty()
  }

  @Test
  fun `nothing is downloaded for a user the server won't let download`() = downloadsTest {
    userRepository.currentStatefulUserFlow.value = userRepository.userFlow.value.let { user ->
      user.copy(permissions = user.permissions.copy(download = false))
    }

    sessionQueue.add(podcast, episode("e1"))

    assertThat(downloads.invocations).isEmpty()
  }

  @Test
  fun `an episode queued again after playing is downloaded again`() = downloadsTest {
    sessionQueue.add(podcast, episode("e1"))
    sessionQueue.pop()

    sessionQueue.add(podcast, episode("e1"))

    assertThat(downloads.invocations).containsExactly(
      Invocation.Download(listOf(DownloadContent.Episode(podcast.id, "e1"))),
      Invocation.Download(listOf(DownloadContent.Episode(podcast.id, "e1"))),
    )
  }

  private fun downloadsTest(block: suspend TestScope.() -> Unit) = runTest(UnconfinedTestDispatcher()) {
    start()
    block()
  }

  private suspend fun TestScope.start() {
    QueuedEpisodeDownloads(
      sessionQueue = sessionQueue,
      downloads = downloads,
      downloadSettings = downloadSettings,
      userRepository = userRepository,
      coroutineScopeHolder = CoroutineScopeHolder { backgroundScope },
    ).onCreate()
  }

  private fun episode(id: String) = PodcastEpisode(
    id = id,
    libraryItemId = podcast.id,
    podcastId = "podcast-media",
    title = "Episode $id",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 60_000L,
    sizeInBytes = 0L,
  )
}
