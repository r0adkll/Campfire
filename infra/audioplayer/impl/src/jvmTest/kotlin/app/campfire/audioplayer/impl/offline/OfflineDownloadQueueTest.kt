// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalCoroutinesApi::class)

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownload.State
import app.campfire.audioplayer.offline.OfflineDownloadKey
import app.campfire.audioplayer.test.fixtures.session
import app.campfire.audioplayer.test.fixtures.track
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.User
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class OfflineDownloadQueueTest {

  private val root: File = createTempDirectory("offline-downloads").toFile()
  private val store = OfflineDownloadStore(root)

  private val tracks = listOf(track(1, 0f, 60f), track(2, 60f, 60f))
  private val book: LibraryItem = session(tracks = tracks).libraryItem

  private val fetches = mutableListOf<FetchRequest>()

  // Not backgroundScope: advanceUntilIdle doesn't run background work. The queue's long-lived command
  // loop sits suspended once idle, so leaving this unparented doesn't hold up the test.
  private val queueScope = CoroutineScope(Job())

  @AfterTest
  fun tearDown() {
    queueScope.cancel()
    root.deleteRecursively()
  }

  private fun TestScope.queue(
    scope: CoroutineScope = queueScope,
    currentUser: User? = downloadingUser,
    retryDelays: List<Duration> = listOf(1.seconds, 1.seconds),
    fetch: suspend (FetchRequest, File, (Long, Long) -> Unit) -> Unit = { request, part, _ ->
      part.appendText(request.url)
    },
  ) = OfflineDownloadQueue(
    store = store,
    fetcher = FileFetcher { request, part, onProgress ->
      fetches += request
      fetch(request, part, onProgress)
    },
    currentUser = { currentUser },
    scope = scope,
    ioDispatcher = StandardTestDispatcher(testScheduler),
    clock = { testScheduler.currentTime },
    retryDelays = retryDelays,
    progressInterval = Duration.ZERO,
  )

  private fun OfflineDownloadQueue.path(track: AudioTrack, episodeId: String? = null) =
    localPathFor(book.id, episodeId, track)

  @Test
  fun `download fetches every track and completes`() = runTest {
    val queue = queue()

    queue.download(book)
    assertThat(queue.getForItem(book).state).isEqualTo(State.Queued)
    advanceUntilIdle()

    val download = queue.getForItem(book)
    assertThat(download.state).isEqualTo(State.Completed)
    assertThat(download.progress.percent).isEqualTo(1f)
    assertThat(fetches.map { it.url }).containsExactly("/track1.m4b", "/track2.m4b")
    assertThat(fetches.map { it.userId }.distinct()).containsExactly("user-1")
    assertThat(File(queue.path(tracks[0])!!).readText()).isEqualTo("/track1.m4b")
    assertThat(File(queue.path(tracks[1])!!).readText()).isEqualTo("/track2.m4b")
    assertThat(queue.observeAll().first().map { it.libraryItemId }).containsExactly(book.id)
  }

  @Test
  fun `progress is published while a track downloads`() = runTest {
    val gate = CompletableDeferred<Unit>()
    val queue = queue { request, part, onProgress ->
      part.writeText("12345")
      onProgress(5, 10)
      if (request.url == "/track1.m4b") gate.await()
    }

    queue.download(book)
    runCurrent()

    val download = queue.observeForItem(book).first()
    assertThat(download.state).isEqualTo(State.Downloading)
    assertThat(download.progress.bytes).isEqualTo(5L)
    assertThat(queue.path(tracks[0])).isNull()

    gate.complete(Unit)
    advanceUntilIdle()
    assertThat(queue.getForItem(book).state).isEqualTo(State.Completed)
  }

  @Test
  fun `a transient failure retries and resumes from the partial file`() = runTest {
    val queue = queue { request, part, _ ->
      if (part.length() == 0L) {
        part.writeText("abc")
        throw IOException("connection reset")
      }
      part.appendText("def")
    }

    queue.download(book)
    advanceUntilIdle()

    assertThat(queue.getForItem(book).state).isEqualTo(State.Completed)
    assertThat(File(queue.path(tracks[0])!!).readText()).isEqualTo("abcdef")
    assertThat(fetches).hasSize(4)
  }

  @Test
  fun `a download that exhausts its retries fails`() = runTest {
    val queue = queue(retryDelays = listOf(1.seconds, 1.seconds)) { _, _, _ -> throw IOException("offline") }

    queue.download(book)
    advanceUntilIdle()

    assertThat(queue.getForItem(book).state).isEqualTo(State.Failed)
    // The first track's three attempts; the rest of the book isn't attempted
    assertThat(fetches.map { it.url }).containsExactly("/track1.m4b", "/track1.m4b", "/track1.m4b")
  }

  @Test
  fun `a permanent failure stays failed until the download is requested again`() = runTest {
    var denied = true
    val queue = queue { request, part, _ ->
      if (denied) throw PermanentFetchException("HTTP 403")
      part.writeText(request.url)
    }

    queue.download(book)
    advanceUntilIdle()
    assertThat(queue.getForItem(book).state).isEqualTo(State.Failed)
    assertThat(fetches).hasSize(1)

    queue.resumeDownloads()
    advanceUntilIdle()
    assertThat(fetches).hasSize(1)

    denied = false
    queue.download(book)
    advanceUntilIdle()
    assertThat(queue.getForItem(book).state).isEqualTo(State.Completed)
  }

  @Test
  fun `delete cancels an active download and removes its files`() = runTest {
    val cancelled = CompletableDeferred<Unit>()
    val queue = queue { _, part, _ ->
      part.writeText("partial")
      try {
        awaitCancellation()
      } finally {
        cancelled.complete(Unit)
      }
    }

    queue.download(book)
    runCurrent()
    assertThat(store.directory(OfflineDownloadKey(book.id)).exists()).isTrue()

    queue.delete(book)
    assertThat(queue.getForItem(book).state).isEqualTo(State.None)
    advanceUntilIdle()

    assertThat(cancelled.isCompleted).isTrue()
    assertThat(root.listFiles()!!.toList()).isEmpty()
    assertThat(queue.observeAll().first()).isEmpty()
  }

  @Test
  fun `an interrupted download resumes after a restart`() = runTest {
    val firstRun = CoroutineScope(Job())
    val interrupted = queue(scope = firstRun) { request, part, _ ->
      if (request.url == "/track1.m4b") {
        part.writeText("one")
      } else {
        part.writeText("tw")
        awaitCancellation()
      }
    }
    interrupted.download(book)
    runCurrent()
    firstRun.cancel()
    advanceUntilIdle()

    val restarted = queue { _, part, _ -> part.appendText("o") }
    val loaded = restarted.getForItem(book)
    assertThat(loaded.state).isEqualTo(State.Queued)
    assertThat(loaded.progress.bytes).isEqualTo(5L)
    assertThat(restarted.path(tracks[0])).isNotNull()
    assertThat(restarted.path(tracks[1])).isNull()

    restarted.resumeDownloads()
    advanceUntilIdle()

    assertThat(restarted.getForItem(book).state).isEqualTo(State.Completed)
    assertThat(File(restarted.path(tracks[1])!!).readText()).isEqualTo("two")
  }

  @Test
  fun `episode downloads are tracked apart from their podcast`() = runTest {
    val queue = queue()
    val episode = PodcastEpisode(
      id = "episode-1",
      libraryItemId = book.id,
      podcastId = book.media.id,
      title = "Episode One",
      addedAtMillis = 0L,
      updatedAtMillis = 0L,
      durationInMillis = 60_000L,
      sizeInBytes = 0L,
      audioTrack = track(1, 0f, 60f),
    )

    queue.downloadEpisode(book, episode)
    advanceUntilIdle()

    assertThat(queue.observeForEpisode(book, episode).first().state).isEqualTo(State.Completed)
    assertThat(queue.observeForEpisodes(book, listOf(episode)).first()[episode.id]!!.episodeId)
      .isEqualTo(episode.id)
    assertThat(queue.getForItem(book).state).isEqualTo(State.None)
    assertThat(queue.path(tracks[0], episodeId = episode.id)).isNotNull()
    assertThat(queue.path(tracks[0])).isNull()

    queue.deleteEpisode(book, episode)
    advanceUntilIdle()
    assertThat(queue.observeForEpisode(book, episode).first().state).isEqualTo(State.None)
  }

  @Test
  fun `deleting by item id removes the book and every episode download`() = runTest {
    val queue = queue()
    val episode = PodcastEpisode(
      id = "episode-1",
      libraryItemId = book.id,
      podcastId = book.media.id,
      title = "Episode One",
      addedAtMillis = 0L,
      updatedAtMillis = 0L,
      durationInMillis = 60_000L,
      sizeInBytes = 0L,
      audioTrack = track(1, 0f, 60f),
    )
    queue.download(book)
    queue.downloadEpisode(book, episode)
    advanceUntilIdle()
    assertThat(queue.observeAll().first()).hasSize(2)

    queue.deleteAllForItemId(book.id)
    advanceUntilIdle()

    assertThat(queue.observeAll().first()).isEmpty()
    assertThat(queue.path(tracks[0])).isNull()
    assertThat(root.listFiles()!!.toList()).isEmpty()
  }

  @Test
  fun `downloads need a logged in user`() = runTest {
    val queue = queue(currentUser = null)

    queue.download(book)
    advanceUntilIdle()

    assertThat(queue.getForItem(book).state).isEqualTo(State.None)
    assertThat(fetches).isEmpty()
    assertThat(root.listFiles()!!.toList()).isEmpty()
  }

  @Test
  fun `a track deleted from disk streams again`() = runTest {
    val queue = queue()
    queue.download(book)
    advanceUntilIdle()

    File(queue.path(tracks[0])!!).delete()

    assertThat(queue.path(tracks[0])).isNull()
    assertThat(queue.observeForItems(listOf(book)).first()[book.id]!!.state).isEqualTo(State.Completed)
  }

  @Test
  fun `unreadable manifests are skipped`() {
    File(root, "li_1/item").mkdirs()
    File(root, "li_1/item/manifest.json").writeText("{ not json")

    assertThat(store.readAll()).isEmpty()
    assertThat(File(root, "li_1/item/manifest.json").exists()).isTrue()
    assertThat(File(root, "li_2").exists()).isFalse()
  }
}
