package app.campfire.podcasts

import androidx.paging.Pager
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.User
import app.campfire.network.AudioBookShelfApi
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.RemotePodcastEpisode
import app.campfire.podcasts.mapping.asDomainModel
import app.campfire.podcasts.mapping.asNetworkModel
import app.campfire.podcasts.paging.RecentEpisodesPagerFactory
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultPodcastsRepository(
  private val pagerFactory: RecentEpisodesPagerFactory,
  private val api: AudioBookShelfApi,
) : PodcastsRepository {

  override fun createLatestEpisodesPager(user: User): Pager<Int, LatestEpisode> {
    return pagerFactory.create(user)
  }

  override suspend fun fetchPodcastFeed(
    rssFeedUrl: String,
  ): Result<List<RemotePodcastEpisode>> {
    return api.getPodcastFeed(rssFeedUrl)
      .map { episodes ->
        episodes
          .filter { it.enclosure.url.isNotBlank() }
          .map { it.asDomainModel() }
      }
  }

  override suspend fun queueEpisodeDownloads(
    libraryItemId: LibraryItemId,
    episodes: List<RemotePodcastEpisode>,
  ): Result<Unit> {
    if (episodes.isEmpty()) return Result.success(Unit)
    return api.downloadPodcastEpisodes(
      libraryItemId = libraryItemId,
      episodes = episodes.map { it.asNetworkModel() },
    )
  }
}
