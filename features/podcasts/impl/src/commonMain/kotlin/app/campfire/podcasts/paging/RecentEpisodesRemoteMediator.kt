package app.campfire.podcasts.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.User
import app.campfire.core.time.FatherTime
import app.campfire.data.PodcastEpisodePageJoin
import app.campfire.data.mapping.asEpisodeAudioTrackDbModelOrNull
import app.campfire.data.mapping.asEpisodeDbModel
import app.campfire.data.mapping.asLibraryItemStubDbModel
import app.campfire.data.mapping.asPodcastMediaDbModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.podcasts.api.LatestEpisode
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias RecentEpisodesRemoteMediatorFactory = (User) -> RecentEpisodesRemoteMediator

private const val MAX_CACHE_TIME_MS = 24L * 60L * 60 * 1000L

@OptIn(ExperimentalPagingApi::class)
@Inject
class RecentEpisodesRemoteMediator(
  @Assisted private val user: User,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
  private val fatherTime: FatherTime,
  private val urlHydrator: UrlHydrator,
) : RemoteMediator<Int, LatestEpisode>(), Cork {

  override val tag: String = "RecentEpisodesRemoteMediator"

  override suspend fun initialize(): InitializeAction {
    val oldestPage = db.podcastEpisodePageQueries.selectOldestPage(
      userId = user.id,
      libraryId = user.selectedLibraryId,
    ).awaitAsOneOrNull()
    val elapsed = fatherTime.nowInEpochMillis() - (oldestPage?.updatedAt ?: 0)
    return if (elapsed > MAX_CACHE_TIME_MS) {
      InitializeAction.LAUNCH_INITIAL_REFRESH
    } else {
      InitializeAction.SKIP_INITIAL_REFRESH
    }
  }

  override suspend fun load(
    loadType: LoadType,
    state: PagingState<Int, LatestEpisode>,
  ): MediatorResult {
    ibark { "Mediator::load($loadType, anchor=${state.anchorPosition}, pages=${state.pages.size})" }
    return try {
      val loadKey = when (loadType) {
        LoadType.REFRESH -> 0
        LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
        LoadType.APPEND -> {
          val nextPage = withContext(dispatcherProvider.databaseRead) {
            db.podcastEpisodePageQueries.selectNextPage(
              userId = user.id,
              libraryId = user.selectedLibraryId,
              mapper = { it ?: -1 },
            ).awaitAsOne().takeIf { it != -1 }
          }
          if (nextPage == null) {
            return MediatorResult.Success(endOfPaginationReached = true)
          }
          nextPage
        }
      }

      val pageSize = state.config.pageSize
      ibark {
        "Mediator::request(library=${user.selectedLibraryId}, page=$loadKey, limit=$pageSize)"
      }
      val response = api.getRecentEpisodes(
        libraryId = user.selectedLibraryId,
        page = loadKey,
        limit = pageSize,
      )

      response.fold(
        onSuccess = { paged ->
          val isLastPage = paged.episodes.size < pageSize
          ibark { "Mediator::response(page=${paged.page}, count=${paged.episodes.size}, last=$isLastPage)" }

          db.transaction {
            if (loadType == LoadType.REFRESH) {
              db.podcastEpisodePageQueries.deleteByLibrary(
                userId = user.id,
                libraryId = user.selectedLibraryId,
              )
            }

            paged.episodes.forEach { episode ->
              // Insert a stub libraryItem first to satisfy the FK from podcastMedia /
              // podcastEpisode. INSERT OR IGNORE preserves any real row already cached
              // by the libraries flow (which has the full libraryItem context).
              db.libraryItemsQueries.insertOrIgnore(
                episode.asLibraryItemStubDbModel(user.serverUrl),
              )
              db.podcastMediaQueries.insertOrIgnore(
                episode.asPodcastMediaDbModel(urlHydrator),
              )
              db.podcastEpisodeQueries.insert(
                episode.asEpisodeDbModel(),
              )
              episode.asEpisodeAudioTrackDbModelOrNull()?.let { audioTrack ->
                db.podcastEpisodeAudioTrackQueries.insert(audioTrack)
              }
            }

            db.podcastEpisodePageQueries.insertPage(
              id = null,
              page = paged.page,
              nextPage = if (isLastPage) null else paged.page + 1,
              libraryId = user.selectedLibraryId,
              userId = user.id,
              updatedAt = fatherTime.nowInEpochMillis(),
            )
            val pageId = db.podcastEpisodePageQueries
              .selectLastPageId()
              .awaitAsOne()

            paged.episodes.forEachIndexed { index, episode ->
              db.podcastEpisodePageQueries.insertPageJoin(
                PodcastEpisodePageJoin(
                  pageId = pageId,
                  pageIndex = index,
                  episodeId = episode.id,
                ),
              )
            }
          }

          MediatorResult.Success(endOfPaginationReached = isLastPage)
        },
        onFailure = { t ->
          ebark(throwable = t) { "RecentEpisodes API/parse failure" }
          MediatorResult.Error(t)
        },
      )
    } catch (e: Exception) {
      ebark(throwable = e) { "RecentEpisodes RemoteMediator Exception" }
      MediatorResult.Error(e)
    }
  }
}
