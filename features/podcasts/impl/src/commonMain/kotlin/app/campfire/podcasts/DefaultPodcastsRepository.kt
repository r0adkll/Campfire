package app.campfire.podcasts

import androidx.paging.Pager
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.User
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.paging.RecentEpisodesPagerFactory
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultPodcastsRepository(
  private val pagerFactory: RecentEpisodesPagerFactory,
) : PodcastsRepository {

  override fun createLatestEpisodesPager(user: User): Pager<Int, LatestEpisode> {
    return pagerFactory.create(user)
  }
}
