package app.campfire.home.ui

import app.campfire.analytics.events.AnalyticEvent
import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.test.mediaProgress
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.ShelfType
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.model.Shelf
import app.campfire.libraries.api.screen.LibraryItemScreen
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.index
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.key
import assertk.assertions.prop
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class HomePresenterTest {

  val navigator = FakeNavigator(HomeScreen)
  val analytics = FakeAnalytics()
  val offlineDownloadManager = FakeOfflineDownloadManager()

  @Test
  fun present_LoadingState() = runTest {
    val repository = FakeHomeRepository(
      homeFeedFlowFactory = { emptyFlow() },
      mediaProgressFlowFactory = { emptyFlow() },
      shelfEntityFlowFactory = { _, _ -> emptyFlow() },
    )
    val presenter = HomePresenter(
      navigator = navigator,
      homeRepository = repository,
      offlineDownloadManager = offlineDownloadManager,
      analytics = analytics,
    )

    presenter.test {
      val state = awaitItem()
      assertThat(state).all {
        homeFeed.isLoading()
        offlineStates.isEmpty()
        progressStates.isEmpty()
      }
    }
  }

  @Test
  fun present_ShelvesNoEntitiesState() = runTest {
    val shelves = listOf(
      shelf("one", "Shelf 1", 5),
      shelf("two", "Shelf 2", 3),
    )
    val repository = FakeHomeRepository(
      homeFeedFlowFactory = { flowOf(FeedResponse.Success(shelves)) },
      mediaProgressFlowFactory = { emptyFlow() },
      shelfEntityFlowFactory = { _, _ -> emptyFlow() },
    )
    val presenter = HomePresenter(
      navigator = navigator,
      homeRepository = repository,
      offlineDownloadManager = offlineDownloadManager,
      analytics = analytics,
    )

    presenter.test {
      // First item is always loading state
      assertThat(awaitItem()).all {
        homeFeed.isLoading()
        offlineStates.isEmpty()
        progressStates.isEmpty()
      }

      // Second should be loaded
      assertThat(awaitItem()).all {
        homeFeed
          .isSuccess()
          .all {
            index(0).isShelf(
              id = "one",
              label = "Shelf 1",
              total = 5,
              entities = { isInstanceOf<LoadState.Loading>() },
            )
            index(1).isShelf(
              id = "two",
              label = "Shelf 2",
              total = 3,
              entities = { isInstanceOf<LoadState.Loading>() },
            )
          }
      }
    }
  }

  @Test
  fun present_ShelvesWithEntitiesState() = runTest {
    val shelves = listOf(
      shelf("one", "Shelf 1", 5),
      shelf("two", "Shelf 2", 3),
    )
    val shelfOneEntities = List(2) { libraryItem(id = "one_$it") }
    val shelfTwoEntities = List(3) { libraryItem(id = "two_$it") }
    val repository = FakeHomeRepository(
      homeFeedFlowFactory = { flowOf(FeedResponse.Success(shelves)) },
      mediaProgressFlowFactory = { emptyFlow() },
      shelfEntityFlowFactory = { shelfId, _ ->
        when (shelfId) {
          "one" -> flowOf(shelfOneEntities)
          "two" -> flowOf(shelfTwoEntities)
          else -> throw IllegalStateException("Unknown shelfId: $shelfId")
        }
      },
    )
    val presenter = HomePresenter(
      navigator = navigator,
      homeRepository = repository,
      offlineDownloadManager = offlineDownloadManager,
      analytics = analytics,
    )

    presenter.test {
      // First item is always loading state
      assertThat(awaitItem()).all {
        homeFeed.isLoading()
        offlineStates.isEmpty()
        progressStates.isEmpty()
      }

      // Third should be Loaded shelves, with loading entities
      assertThat(awaitItem()).all {
        homeFeed
          .isSuccess()
          .all {
            index(0).isShelf(
              id = "one",
              label = "Shelf 1",
              total = 5,
              entities = {
                isInstanceOf<LoadState.Loaded<List<*>>>()
                  .prop(LoadState.Loaded<List<*>>::data)
                  .containsExactly(
                    *shelfOneEntities.toTypedArray(),
                  )
              },
            )
            index(1).isShelf(
              id = "two",
              label = "Shelf 2",
              total = 3,
              entities = {
                isInstanceOf<LoadState.Loaded<List<*>>>()
                  .prop(LoadState.Loaded<List<*>>::data)
                  .containsExactly(
                    *shelfTwoEntities.toTypedArray(),
                  )
              },
            )
          }
      }
    }
  }

  @Test
  fun present_UserMediaProgress_mapsToLoadedItems() = runTest {
    val shelves = listOf(
      shelf("one", "Shelf 1", 5),
      shelf("two", "Shelf 2", 3),
    )
    val shelfOneEntities = List(2) { libraryItem(id = "one_$it") }
    val shelfTwoEntities = List(3) { libraryItem(id = "two_$it") }
    val mediaProgress =
      List(2) { mediaProgress(libraryItemId = "one_$it") }.associateBy { it.libraryItemId } +
        List(3) { mediaProgress(libraryItemId = "two_$it") }.associateBy { it.libraryItemId }
    val repository = FakeHomeRepository(
      homeFeedFlowFactory = { flowOf(FeedResponse.Success(shelves)) },
      mediaProgressFlowFactory = { flowOf(mediaProgress) },
      shelfEntityFlowFactory = { shelfId, _ ->
        when (shelfId) {
          "one" -> flowOf(shelfOneEntities)
          "two" -> flowOf(shelfTwoEntities)
          else -> throw IllegalStateException("Unknown shelfId: $shelfId")
        }
      },
    )
    val presenter = HomePresenter(
      navigator = navigator,
      homeRepository = repository,
      offlineDownloadManager = offlineDownloadManager,
      analytics = analytics,
    )

    presenter.test {
      // Pull Loading State
      awaitItem()

      assertThat(awaitItem()).all {
        progressStates.all {
          key("one_0").isNotNull()
          key("one_1").isNotNull()
          key("two_0").isNotNull()
          key("two_1").isNotNull()
          key("two_2").isNotNull()
        }
      }
    }
  }

  @Test
  fun present_OfflineDownloads_mapsToLoadedItems() = runTest {
    val shelves = listOf(
      shelf("one", "Shelf 1", 5),
      shelf("two", "Shelf 2", 3),
    )
    val shelfOneEntities = List(2) { libraryItem(id = "one_$it") }
    val shelfTwoEntities = List(3) { libraryItem(id = "two_$it") }
    val offlineDownloads =
      List(2) { OfflineDownload(libraryItemId = "one_$it") }.associateBy { it.libraryItemId } +
        List(3) { OfflineDownload(libraryItemId = "two_$it") }.associateBy { it.libraryItemId }
    offlineDownloadManager.observeForItemsFlow.emit(offlineDownloads)
    val repository = FakeHomeRepository(
      homeFeedFlowFactory = { flowOf(FeedResponse.Success(shelves)) },
      mediaProgressFlowFactory = { emptyFlow() },
      shelfEntityFlowFactory = { shelfId, _ ->
        when (shelfId) {
          "one" -> flowOf(shelfOneEntities)
          "two" -> flowOf(shelfTwoEntities)
          else -> throw IllegalStateException("Unknown shelfId: $shelfId")
        }
      },
    )
    val presenter = HomePresenter(
      navigator = navigator,
      homeRepository = repository,
      offlineDownloadManager = offlineDownloadManager,
      analytics = analytics,
    )

    presenter.test {
      // Pull Loading State
      awaitItem()

      assertThat(awaitItem()).all {
        offlineStates.all {
          key("one_0").isNotNull()
          key("one_1").isNotNull()
          key("two_0").isNotNull()
          key("two_1").isNotNull()
          key("two_2").isNotNull()
        }
      }
    }
  }

  @Test
  fun eventSink_OpenLibraryItem_analyticsAndNavigates() = runTest {
    val repository = FakeHomeRepository(
      homeFeedFlowFactory = { emptyFlow() },
      mediaProgressFlowFactory = { emptyFlow() },
      shelfEntityFlowFactory = { _, _ -> emptyFlow() },
    )
    val presenter = HomePresenter(
      navigator = navigator,
      homeRepository = repository,
      offlineDownloadManager = offlineDownloadManager,
      analytics = analytics,
    )
    val libraryItemId = "test_library_item"
    val sharedTransitionKey = "shared_key"
    val libraryItem = libraryItem(libraryItemId)

    presenter.test {
      val state = awaitItem()

      state.eventSink(HomeUiEvent.OpenLibraryItem(libraryItem, sharedTransitionKey))

      assertThat(navigator.awaitNextScreen()).isEqualTo(LibraryItemScreen(libraryItemId, sharedTransitionKey))
      assertThat(analytics.events.first()).prop(AnalyticEvent::eventName).isEqualTo("library_item_selected")
    }
  }

  @Test
  fun eventSink_OpenAuthor_analyticsAndNavigates() = runTest {
    val repository = FakeHomeRepository(
      homeFeedFlowFactory = { emptyFlow() },
      mediaProgressFlowFactory = { emptyFlow() },
      shelfEntityFlowFactory = { _, _ -> emptyFlow() },
    )
    val presenter = HomePresenter(
      navigator = navigator,
      homeRepository = repository,
      offlineDownloadManager = offlineDownloadManager,
      analytics = analytics,
    )
    val authorId = "test_authorId"
    val authorName = "test_author_name"
    val author = author(authorId, authorName)

    presenter.test {
      val state = awaitItem()

      state.eventSink(HomeUiEvent.OpenAuthor(author))

      assertThat(navigator.awaitNextScreen()).isEqualTo(AuthorDetailScreen(authorId, authorName))
      assertThat(analytics.events.first()).prop(AnalyticEvent::eventName).isEqualTo("author_selected")
    }
  }

  @Test
  fun eventSink_OpenSeries_analyticsAndNavigates() = runTest {
    val repository = FakeHomeRepository(
      homeFeedFlowFactory = { emptyFlow() },
      mediaProgressFlowFactory = { emptyFlow() },
      shelfEntityFlowFactory = { _, _ -> emptyFlow() },
    )
    val presenter = HomePresenter(
      navigator = navigator,
      homeRepository = repository,
      offlineDownloadManager = offlineDownloadManager,
      analytics = analytics,
    )
    val seriesId = "test_seriesId"
    val seriesName = "test_series_name"
    val series = series(seriesId, seriesName)

    presenter.test {
      val state = awaitItem()

      state.eventSink(HomeUiEvent.OpenSeries(series))

      assertThat(navigator.awaitNextScreen()).isEqualTo(SeriesDetailScreen(seriesId, seriesName))
      assertThat(analytics.events.first()).prop(AnalyticEvent::eventName).isEqualTo("series_selected")
    }
  }
}

private fun shelf(
  id: String,
  label: String,
  total: Int,
  type: ShelfType = ShelfType.BOOK,
  order: Int = 0,
) = Shelf(id, label, total, type, order)

typealias AssertHomeFeedResponse = Assert<FeedResponse<out PersistentList<UiShelf<*>>>>

private val Assert<HomeUiState>.homeFeed: AssertHomeFeedResponse
  get() = prop(HomeUiState::homeFeed)

private val Assert<HomeUiState>.offlineStates: Assert<ImmutableMap<LibraryItemId, OfflineDownload>>
  get() = prop(HomeUiState::offlineStates)

private val Assert<HomeUiState>.progressStates: Assert<ImmutableMap<LibraryItemId, MediaProgress>>
  get() = prop(HomeUiState::progressStates)

private fun AssertHomeFeedResponse.isLoading(): Assert<FeedResponse.Loading> {
  return isInstanceOf()
}

private fun AssertHomeFeedResponse.isSuccess(): Assert<PersistentList<UiShelf<*>>> {
  return isInstanceOf<FeedResponse.Success<PersistentList<UiShelf<*>>>>()
    .prop(FeedResponse.Success<PersistentList<UiShelf<*>>>::data)
}

private inline fun Assert<UiShelf<*>>.isShelf(
  id: String,
  label: String,
  total: Int,
  crossinline entities: Assert<LoadState<*>>.() -> Unit,
) {
  return all {
    prop(UiShelf<*>::id).isEqualTo(id)
    prop(UiShelf<*>::label).isEqualTo(label)
    prop(UiShelf<*>::total).isEqualTo(total)
    prop(UiShelf<*>::entities).entities()
  }
}
