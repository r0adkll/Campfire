// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.InitializeAction
import androidx.paging.RemoteMediator.MediatorResult
import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.time.GrandFatherTime
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.network.models.PagedRecentEpisodesResponse
import app.campfire.network.models.Podcast
import app.campfire.network.models.RecentPodcastEpisode
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsList
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalPagingApi::class)
class RecentEpisodesRemoteMediatorTest {

  private val api = FakeAudioBookShelfApi()
  private val testUser = user("user")

  @Test
  fun `opening the feed refreshes it even right after a refresh`() = mediatorTest { mediator, _ ->
    api.recentEpisodesResult = { Result.success(page(episode("ep_1"))) }
    mediator.load(LoadType.REFRESH, emptyState())

    assertThat(mediator.initialize()).isEqualTo(InitializeAction.LAUNCH_INITIAL_REFRESH)
  }

  @Test
  fun `a refresh replaces the cached feed with the server's newest episodes`() = mediatorTest { mediator, db ->
    api.recentEpisodesResult = { Result.success(page(episode("ep_1"))) }
    mediator.load(LoadType.REFRESH, emptyState())

    api.recentEpisodesResult = { Result.success(page(episode("ep_2"), episode("ep_1"))) }
    val result = mediator.load(LoadType.REFRESH, emptyState())

    assertThat(result).isInstanceOf<MediatorResult.Success>()
    assertThat(db.feedEpisodeIds()).containsExactly("ep_2", "ep_1")
    assertThat(api.recentEpisodesRequests.map { it.page }).containsExactly(0, 0)
  }

  @Test
  fun `a failed refresh keeps the cached feed`() = mediatorTest { mediator, db ->
    api.recentEpisodesResult = { Result.success(page(episode("ep_1"))) }
    mediator.load(LoadType.REFRESH, emptyState())

    api.recentEpisodesResult = { Result.failure(IllegalStateException("Unable to resolve host")) }
    val result = mediator.load(LoadType.REFRESH, emptyState())

    assertThat(result).isInstanceOf<MediatorResult.Error>()
    assertThat(db.feedEpisodeIds()).containsExactly("ep_1")
  }

  @Test
  fun `appending past the last page stops without asking the server`() = mediatorTest { mediator, _ ->
    api.recentEpisodesResult = { Result.success(page(episode("ep_1"))) }
    mediator.load(LoadType.REFRESH, emptyState())
    api.recentEpisodesRequests.clear()

    val result = mediator.load(LoadType.APPEND, emptyState())

    assertThat(result).isInstanceOf<MediatorResult.Success>()
      .prop(MediatorResult.Success::endOfPaginationReached)
      .isTrue()
    assertThat(api.recentEpisodesRequests).isEmpty()
  }

  private fun mediatorTest(
    block: suspend TestScope.(RecentEpisodesRemoteMediator, CampfireDatabase) -> Unit,
  ) = runTest {
    val driver = createDriver()
    try {
      val db = DatabaseFactory(driver).build()
      val mediator = RecentEpisodesRemoteMediator(
        user = testUser,
        api = api,
        db = db,
        dispatcherProvider = asTestDispatcherProvider(),
        fatherTime = GrandFatherTime,
        urlHydrator = FakeUrlHydrator(),
      )
      block(mediator, db)
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
    libraryItemId = "li_1",
    podcastId = "pod_1",
    id = id,
    title = "Episode $id",
    addedAt = 0,
    updatedAt = 0,
    podcast = Podcast(id = "pod_1"),
    libraryId = testUser.selectedLibraryId,
  )

  private companion object {
    const val PAGE_SIZE = 50
  }
}
