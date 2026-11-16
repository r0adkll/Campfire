// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.PlaybackControllerSession
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.common.test.session
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.User
import app.campfire.core.time.GrandFatherTime
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.network.ApiException
import app.campfire.network.models.PagedRecentEpisodesResponse
import app.campfire.network.models.Podcast
import app.campfire.network.models.PodcastEpisodeDownload
import app.campfire.network.models.RecentPodcastEpisode
import app.campfire.network.test.DeletedPodcastEpisode
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import app.campfire.podcasts.api.EpisodeDownloadsSnapshot
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.podcasts.downloads.RemoteEpisodeDownloadSink
import app.campfire.podcasts.paging.RecentEpisodesPagerFactory
import app.campfire.podcasts.paging.RecentEpisodesRemoteMediator
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalPagingApi::class)
class DefaultPodcastsRepositoryTest {

  private val api = FakeAudioBookShelfApi()
  private val sessionQueue = FakeSessionQueue()
  private val downloads = FakeDownloads()
  private val sessionsRepository = FakeSessionsRepository()
  private val playbackController = FakePlaybackController()
  private val testUser = user("user")

  @Test
  fun `deleting an episode deletes it on the server and forgets it here`() = repositoryTest { repository, db ->
    api.deletePodcastEpisodeResult = { _, _ -> Result.success(Unit) }
    sessionQueue.add(ITEM_ID, "ep_1")

    val result = repository.deleteEpisode(ITEM_ID, "ep_1", hardDelete = true)

    assertThat(result.isSuccess).isTrue()
    assertThat(api.deletedPodcastEpisodes).containsExactly(DeletedPodcastEpisode(ITEM_ID, "ep_1", hard = true))
    assertThat(db.feedEpisodeIds()).containsExactly("ep_2")
    assertThat(db.podcastEpisodeQueries.selectForId("ep_1").awaitAsOneOrNull()).isNull()
    assertThat(sessionQueue.queue).isEmpty()
    assertThat(downloads.invocations)
      .containsExactly(FakeDownloads.Invocation.Delete(DownloadContent.Episode(ITEM_ID, "ep_1")))
  }

  @Test
  fun `deleting the playing episode stops its session`() = repositoryTest { repository, _ ->
    api.deletePodcastEpisodeResult = { _, _ -> Result.success(Unit) }
    sessionsRepository.currentSessionFlow.value = session(libraryItem = libraryItem(id = ITEM_ID))
      .copy(episodeId = "ep_1")

    repository.deleteEpisode(ITEM_ID, "ep_1", hardDelete = false)

    assertThat(playbackController.session)
      .isEqualTo(PlaybackControllerSession.Stopped(ITEM_ID, episodeId = "ep_1"))
    assertThat(sessionsRepository.invocations)
      .contains(FakeSessionsRepository.Invocation.MarkDeleted(ITEM_ID, "ep_1"))
  }

  @Test
  fun `deleting an episode leaves another one playing`() = repositoryTest { repository, _ ->
    api.deletePodcastEpisodeResult = { _, _ -> Result.success(Unit) }
    sessionsRepository.currentSessionFlow.value = session(libraryItem = libraryItem(id = ITEM_ID))
      .copy(episodeId = "ep_2")

    repository.deleteEpisode(ITEM_ID, "ep_1", hardDelete = false)

    assertThat(playbackController.session).isEqualTo(PlaybackControllerSession.None)
  }

  @Test
  fun `a failed delete keeps the episode playing`() = repositoryTest { repository, _ ->
    api.deletePodcastEpisodeResult = { _, _ -> Result.failure(ApiException(500)) }
    sessionsRepository.currentSessionFlow.value = session(libraryItem = libraryItem(id = ITEM_ID))
      .copy(episodeId = "ep_1")

    repository.deleteEpisode(ITEM_ID, "ep_1", hardDelete = false)

    assertThat(playbackController.session).isEqualTo(PlaybackControllerSession.None)
    assertThat(sessionsRepository.invocations).isEmpty()
  }

  @Test
  fun `an open podcast page hears about the deleted episode`() = repositoryTest { repository, db ->
    api.deletePodcastEpisodeResult = { _, _ -> Result.success(Unit) }

    // What a podcast's page watches: its item and podcast rows, not its episodes
    db.libraryItemsQueries.selectForPodcastId(ITEM_ID).asFlow().test {
      awaitItem()

      repository.deleteEpisode(ITEM_ID, "ep_1", hardDelete = false)

      awaitItem()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `an episode already gone from the server is forgotten here too`() = repositoryTest { repository, db ->
    api.deletePodcastEpisodeResult = { _, _ -> Result.failure(ApiException(404)) }

    val result = repository.deleteEpisode(ITEM_ID, "ep_1", hardDelete = false)

    assertThat(result.isSuccess).isTrue()
    assertThat(db.feedEpisodeIds()).containsExactly("ep_2")
  }

  @Test
  fun `a refused delete says so and keeps the episode`() = repositoryTest { repository, db ->
    api.deletePodcastEpisodeResult = { _, _ -> Result.failure(ApiException(403)) }

    val result = repository.deleteEpisode(ITEM_ID, "ep_1", hardDelete = false)

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<ItemChangeNotAllowedException>()
    assertThat(db.feedEpisodeIds()).containsExactly("ep_1", "ep_2")
    assertThat(db.podcastEpisodeQueries.selectForId("ep_1").awaitAsOneOrNull()).isNotNull()
    assertThat(downloads.invocations).isEmpty()
  }

  @Test
  fun `a failed delete keeps the episode`() = repositoryTest { repository, db ->
    api.deletePodcastEpisodeResult = { _, _ -> Result.failure(IllegalStateException("Unable to resolve host")) }

    val result = repository.deleteEpisode(ITEM_ID, "ep_1", hardDelete = false)

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<IllegalStateException>()
    assertThat(db.feedEpisodeIds()).containsExactly("ep_1", "ep_2")
  }

  private fun repositoryTest(
    block: suspend TestScope.(DefaultPodcastsRepository, CampfireDatabase) -> Unit,
  ) = runTest {
    val driver = createDriver()
    try {
      val db = DatabaseFactory(driver).build()
      val dispatcherProvider = asTestDispatcherProvider()
      val mediator = { user: User ->
        RecentEpisodesRemoteMediator(
          user = user,
          api = api,
          db = db,
          dispatcherProvider = dispatcherProvider,
          fatherTime = GrandFatherTime,
          urlHydrator = FakeUrlHydrator(),
        )
      }

      // The feed, as Latest last loaded it
      api.recentEpisodesResult = { Result.success(page(episode("ep_1"), episode("ep_2"))) }
      mediator(testUser).load(LoadType.REFRESH, emptyState())

      val repository = DefaultPodcastsRepository(
        pagerFactory = RecentEpisodesPagerFactory(
          remoteMediatorFactory = mediator,
          db = db,
          dispatcherProvider = dispatcherProvider,
          urlHydrator = FakeUrlHydrator(),
        ),
        api = api,
        downloadSink = NoOpDownloadSink,
        db = db,
        sessionQueue = sessionQueue,
        sessionsRepository = sessionsRepository,
        playbackController = playbackController,
        downloads = downloads,
        dispatcherProvider = dispatcherProvider,
      )
      block(repository, db)
    } finally {
      driver.close()
    }
  }

  private suspend fun CampfireDatabase.feedEpisodeIds(): List<String> {
    return podcastEpisodePageQueries
      .selectEpisodesWithLimitOffset(
        userId = testUser.id,
        libraryId = testUser.selectedLibraryId,
        limit = 100,
        offset = 0,
      )
      .awaitAsList()
      .map { it.id }
  }

  private fun emptyState() = PagingState<Int, LatestEpisode>(
    pages = emptyList(),
    anchorPosition = null,
    config = PagingConfig(pageSize = PAGE_SIZE),
    leadingPlaceholderCount = 0,
  )

  private fun page(vararg episodes: RecentPodcastEpisode) = PagedRecentEpisodesResponse(
    episodes = episodes.toList(),
    limit = PAGE_SIZE,
    page = 0,
  )

  private fun episode(id: String) = RecentPodcastEpisode(
    libraryItemId = ITEM_ID,
    podcastId = "pod_1",
    id = id,
    title = "Episode $id",
    addedAt = 0,
    updatedAt = 0,
    podcast = Podcast(id = "pod_1"),
    libraryId = testUser.selectedLibraryId,
  )

  private object NoOpDownloadSink : RemoteEpisodeDownloadSink {
    override fun onQueued(download: PodcastEpisodeDownload) = Unit
    override fun onStarted(download: PodcastEpisodeDownload) = Unit
    override fun onFinished(download: PodcastEpisodeDownload) = Unit
    override fun onQueueCleared(libraryItemId: LibraryItemId) = Unit
    override fun applySnapshot(libraryId: LibraryId, snapshot: EpisodeDownloadsSnapshot) = Unit
  }

  private companion object {
    const val ITEM_ID = "li_1"
    const val PAGE_SIZE = 50
  }
}
