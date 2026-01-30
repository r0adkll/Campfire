package app.campfire.series.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.Series
import app.campfire.core.model.User
import app.campfire.core.session.serverUrl
import app.campfire.core.settings.SortDirection
import app.campfire.core.time.FatherTime
import app.campfire.core.util.runIfNotNull
import app.campfire.data.SeriesBookJoin
import app.campfire.data.SeriesPageJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.LibraryItemFilter
import app.campfire.network.nextPage
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias SeriesRemoteMediatorFactory = (User, SeriesPagingInput) -> SeriesRemoteMediator

private const val MAX_CACHE_TIME_MS = 24L * 60L * 60 * 1000L

@OptIn(ExperimentalPagingApi::class)
@Inject
class SeriesRemoteMediator(
  @Assisted private val user: User,
  @Assisted private val input: SeriesPagingInput,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
  private val fatherTime: FatherTime,
) : RemoteMediator<Int, Series>(), Cork {

  override val tag: String = "SeriesRemoteMediator"

  override suspend fun initialize(): InitializeAction {
    val oldestPage = db.seriesPageQueries.selectOldestPage(
      input = input.databaseKey,
      userId = user.id,
      libraryId = user.selectedLibraryId,
    ).awaitAsOneOrNull()
    val elapsed = fatherTime.nowInEpochMillis() - (oldestPage?.updatedAt ?: 0)
    if (elapsed > MAX_CACHE_TIME_MS) {
      return InitializeAction.LAUNCH_INITIAL_REFRESH
    } else {
      return InitializeAction.SKIP_INITIAL_REFRESH
    }
  }

  override suspend fun load(
    loadType: LoadType,
    state: PagingState<Int, Series>,
  ): MediatorResult {
    ibark { "Mediator::load($loadType, anchor=${state.anchorPosition}, pages=${state.pages.size})" }
    return try {
      val loadKey = when (loadType) {
        LoadType.REFRESH -> null
        LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
        LoadType.APPEND -> {
          val nextPage = withContext(dispatcherProvider.databaseRead) {
            db.seriesPageQueries.selectNextPage(
              input = input.databaseKey,
              userId = user.id,
              libraryId = user.selectedLibraryId,
              mapper = { it ?: -1 },
            ).awaitAsOne().takeIf { it != -1 }
          }

          if (nextPage == null) {
            return MediatorResult.Success(
              endOfPaginationReached = true,
            )
          }

          nextPage
        }
      } ?: 0

      ibark { "Mediator::loadKey($loadKey)" }

      // Load the page from the network
      val response = api.getSeries(
        libraryId = user.selectedLibraryId,
        filter = input.filter?.let {
          LibraryItemFilter(it.group, it.value)
        },
        sortMode = input.sortMode.networkKey.seriesSortKey,
        sortDescending = input.sortDirection == SortDirection.Descending,
        page = loadKey,
        limit = state.config.pageSize,
      )

      response.fold(
        onSuccess = { pagedResponse ->
          ibark { "Mediator::response(page=${pagedResponse.page}, nextPage=${pagedResponse.nextPage})" }

          db.transaction {
            if (loadType == LoadType.REFRESH) {
              db.seriesPageQueries.deleteByInput(
                input = input.databaseKey,
                userId = user.id,
                libraryId = user.selectedLibraryId,
              )
            }

            // Insert items
            pagedResponse.data.forEach { series ->
              // Insert Series
              db.seriesQueries.insertOrIgnore(series.asDbModel(user.id, user.selectedLibraryId))

              // Insert the series books
              series.books?.forEachIndexed { index, book ->
                val libraryItem = book.asDbModel(user.serverUrl)
                val media = book.media.asDbModel(book.id, fallbackSeriesSequence = index + 1)

                // If these items exist, lets not overwrite their metadata
                db.libraryItemsQueries.insertOrIgnore(libraryItem)
                db.mediaQueries.insertOrIgnore(media)

                // Make sure we keep our item series sequence up to date
                runIfNotNull(
                  media.metadata_series_id,
                  media.metadata_series_name,
                  media.metadata_series_sequence,
                ) { id, name, sequence ->
                  db.mediaQueries.updateSeriesSequence(id, name, sequence, book.id)
                }

                // Insert junction entry
                db.seriesBookJoinQueries.insert(
                  SeriesBookJoin(
                    seriesId = series.id,
                    libraryItemId = book.id,
                  ),
                )
              }
            }

            // Insert the Page + Joins
            db.seriesPageQueries.insertPage(
              id = null, // Auto-incrementing
              input = input.databaseKey,
              page = pagedResponse.page,
              nextPage = pagedResponse.nextPage,
              total = pagedResponse.total,
              libraryId = user.selectedLibraryId,
              userId = user.id,
              updatedAt = fatherTime.nowInEpochMillis(),
            )
            val pageId = db.seriesPageQueries
              .selectLastPageId()
              .awaitAsOne()

            ibark { "Inserted page ${pagedResponse.page} with rowId[$pageId]" }

            pagedResponse.data.forEachIndexed { index, item ->
              db.seriesPageQueries.insertPageJoin(
                SeriesPageJoin(
                  pageId = pageId,
                  seriesId = item.id,
                  pageIndex = index,
                ),
              )
            }
          }

          MediatorResult.Success(
            endOfPaginationReached = pagedResponse.nextPage == null,
          )
        },
        onFailure = { t ->
          MediatorResult.Error(t)
        },
      )
    } catch (e: Exception) {
      ebark(throwable = e) { "LibraryItem RemoteMediator Exception" }
      MediatorResult.Error(e)
    }
  }
}
