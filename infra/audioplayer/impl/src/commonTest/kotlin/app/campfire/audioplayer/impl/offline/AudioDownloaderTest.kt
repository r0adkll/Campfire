// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.impl.offline.FakeOfflineDownloadManager.Invocation
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.RescanResult
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class AudioDownloaderTest {

  private val manager = FakeOfflineDownloadManager()
  private val items = MapLibraryItemRepository()

  private val book = libraryItem(id = "book")
  private val episodeOne = episode("ep-1")
  private val episodeTwo = episode("ep-2")
  private val podcast = book.copy(id = "podcast", media = podcastMedia(episodeOne, episodeTwo))

  init {
    items += book
    items += podcast
  }

  private fun TestScope.downloader() = AudioDownloader(manager, items, CoroutineScopeHolder { this })

  @Test
  fun `handles audiobooks and episodes, not ebooks`() = runTest {
    val downloader = downloader()

    assertThat(downloader.handles(DownloadContent.Audiobook("book"))).isTrue()
    assertThat(downloader.handles(DownloadContent.Episode("podcast", "ep-1"))).isTrue()
    assertThat(downloader.handles(DownloadContent.Ebook("book"))).isFalse()
  }

  @Test
  fun `every audio download shows, books and episodes alike`() = runTest {
    manager.observeAllFlow.emit(
      listOf(
        OfflineDownload("book", OfflineDownload.State.Completed),
        OfflineDownload("podcast", OfflineDownload.State.Downloading, episodeId = "ep-1"),
        OfflineDownload("none"),
      ),
    )

    val downloads = downloader().observeAll().first()

    assertThat(downloads.map { it.content to it.state }).containsExactly(
      DownloadContent.Audiobook("book") to Download.State.Downloaded,
      DownloadContent.Episode("podcast", "ep-1") to Download.State.Downloading,
    )
  }

  @Test
  fun `a book's download is observed from its item`() = runTest {
    manager.observeForItemFlow.emit(OfflineDownload("book", OfflineDownload.State.Queued))

    val downloads = downloader().observe(setOf(DownloadContent.Audiobook("book"))).first()

    assertThat(downloads.mapValues { it.value.state })
      .containsOnly(DownloadContent.Audiobook("book") to Download.State.Queued)
  }

  @Test
  fun `content not downloaded is absent`() = runTest {
    manager.observeForItemFlow.emit(OfflineDownload("book"))

    assertThat(downloader().observe(setOf(DownloadContent.Audiobook("book"))).first()).isEmpty()
  }

  @Test
  fun `one episode is observed on its own`() = runTest {
    manager.observeForEpisodeFlow.emit(
      OfflineDownload("podcast", OfflineDownload.State.Completed, episodeId = "ep-1"),
    )

    val downloads = downloader().observe(setOf(DownloadContent.Episode("podcast", "ep-1"))).first()

    assertThat(downloads.keys).containsOnly(DownloadContent.Episode("podcast", "ep-1"))
  }

  @Test
  fun `several episodes are observed together`() = runTest {
    manager.observeForEpisodesFlow.emit(
      mapOf(
        "ep-1" to OfflineDownload("podcast", OfflineDownload.State.Completed, episodeId = "ep-1"),
        "ep-2" to OfflineDownload("podcast", episodeId = "ep-2"),
      ),
    )

    val downloads = downloader().observe(
      setOf(DownloadContent.Episode("podcast", "ep-1"), DownloadContent.Episode("podcast", "ep-2")),
    ).first()

    assertThat(downloads.keys).containsOnly(DownloadContent.Episode("podcast", "ep-1"))
  }

  @Test
  fun `an item that can't be found has no downloads`() = runTest {
    val downloader = downloader()

    assertThat(downloader.observe(setOf(DownloadContent.Audiobook("gone"))).first()).isEmpty()
    assertThat(downloader.observe(emptySet()).first()).isEmpty()
    downloader.download(listOf(DownloadContent.Audiobook("gone")))
    advanceUntilIdle()

    assertThat(manager.invocations).isEmpty()
  }

  @Test
  fun `downloads go to the manager with their item`() = runTest {
    downloader().download(
      listOf(
        DownloadContent.Audiobook("book"),
        DownloadContent.Episode("podcast", "ep-1"),
        DownloadContent.Episode("podcast", "ep-2"),
      ),
    )
    advanceUntilIdle()

    assertThat(manager.invocations).containsExactly(
      Invocation.Download(book),
      Invocation.DownloadEpisode(podcast, episodeOne),
      Invocation.DownloadEpisode(podcast, episodeTwo),
    )
  }

  @Test
  fun `stopping and deleting go to the manager in the order asked`() = runTest {
    val downloader = downloader()

    downloader.stop(DownloadContent.Audiobook("book"))
    downloader.stop(DownloadContent.Episode("podcast", "ep-1"))
    downloader.delete(DownloadContent.Audiobook("book"))
    downloader.delete(DownloadContent.Episode("podcast", "ep-2"))
    advanceUntilIdle()

    assertThat(manager.invocations).containsExactly(
      Invocation.Stop(book),
      Invocation.StopEpisode(podcast, episodeOne),
      Invocation.Delete(book),
      Invocation.DeleteEpisode(podcast, episodeTwo),
    )
  }

  @Test
  fun `deleting an item goes straight to the manager`() = runTest {
    downloader().deleteAll("book")

    assertThat(manager.invocations).containsExactly(Invocation.DeleteAllForItemId("book"))
  }

  @Test
  fun `an audio download keeps its sizes and progress`() {
    val download = OfflineDownload(
      libraryItemId = "book",
      state = OfflineDownload.State.Downloading,
      updateTimeMs = 42L,
      contentLength = 100L,
      progress = OfflineDownload.Progress(bytes = 25L, percent = 0.25f),
    ).asDownload()

    assertThat(download).isEqualTo(
      Download(
        content = DownloadContent.Audiobook("book"),
        state = Download.State.Downloading,
        bytes = 25L,
        totalBytes = 100L,
        progress = 0.25f,
        updatedAtMs = 42L,
      ),
    )
  }

  @Test
  fun `every audio state has a download state`() {
    val states = OfflineDownload.State.entries.associateWith { OfflineDownload("book", it).asDownload()?.state }

    assertThat(states).containsOnly(
      OfflineDownload.State.None to null,
      OfflineDownload.State.Queued to Download.State.Queued,
      OfflineDownload.State.Stopped to Download.State.Stopped,
      OfflineDownload.State.Downloading to Download.State.Downloading,
      OfflineDownload.State.Completed to Download.State.Downloaded,
      OfflineDownload.State.Failed to Download.State.Failed,
    )
  }

  @Test
  fun `progress that can't be told is null`() {
    val download = OfflineDownload(
      libraryItemId = "book",
      state = OfflineDownload.State.Downloading,
      progress = OfflineDownload.Progress(bytes = 25L, percent = 0f, indeterminate = true),
    ).asDownload()

    assertThat(download?.progress).isNull()
  }

  private fun episode(id: String) = PodcastEpisode(
    id = id,
    libraryItemId = "podcast",
    podcastId = "podcast-media",
    title = "Episode $id",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = 0L,
  )

  private fun podcastMedia(vararg episodes: PodcastEpisode) = Media.Podcast(
    id = "podcast-media",
    metadata = Media.Metadata.Podcast(
      title = "Podcast",
      author = null,
      description = null,
      releaseDate = null,
      genres = emptyList(),
      feedUrl = null,
      imageUrl = null,
      itunesPageUrl = null,
      itunesId = null,
      itunesArtistId = null,
      isExplicit = false,
      language = null,
      podcastType = null,
    ),
    coverImageUrl = "",
    coverPath = null,
    tags = emptyList(),
    sizeInBytes = 0L,
    episodes = episodes.toList(),
  )

  private class MapLibraryItemRepository : LibraryItemRepository {
    private val items = mutableMapOf<LibraryItemId, LibraryItem>()

    operator fun plusAssign(item: LibraryItem) {
      items[item.id] = item
    }

    override fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem> = emptyFlow()

    override suspend fun getLibraryItem(itemId: LibraryItemId): LibraryItem =
      items[itemId] ?: throw NoSuchElementException(itemId)

    override suspend fun deleteLibraryItem(itemId: LibraryItemId, hardDelete: Boolean) = Result.success(Unit)
    override suspend fun rescanLibraryItem(itemId: LibraryItemId) = Result.success(RescanResult.UpToDate)
  }
}
