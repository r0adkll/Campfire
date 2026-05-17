package app.campfire.podcasts.ui.add

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.core.model.User
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryFolder
import app.campfire.podcasts.api.PodcastDraft
import app.campfire.podcasts.api.PodcastFeedDetails
import app.campfire.podcasts.api.PodcastSearchResult
import app.campfire.podcasts.api.screen.AddPodcastBuilderScreen
import app.campfire.podcasts.api.screen.AddPodcastScreen
import app.campfire.podcasts.ui.FakeLibraryRepository
import app.campfire.podcasts.ui.FakePodcastsRepository
import app.campfire.user.test.FakeUserRepository
import app.campfire.user.test.fixtures.user
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private const val LIB = "lib_1"
private val SCREEN = AddPodcastScreen(libraryId = LIB)
private val SAMPLE_RESULT = PodcastSearchResult(
  itunesId = "1234",
  itunesArtistId = "5678",
  title = "Sample",
  author = "Author",
  descriptionHtml = null,
  descriptionPlain = "About",
  coverUrl = "https://example.com/cover.jpg",
  feedUrl = "https://example.com/feed.xml",
  itunesPageUrl = null,
  releaseDateIso = null,
  genres = listOf("Tech"),
  trackCount = 42,
  explicit = false,
)
private val SAMPLE_DRAFT = PodcastDraft(
  title = "Pasted Feed",
  author = "Some Host",
  descriptionHtml = null,
  descriptionPlain = "Pasted",
  coverUrl = null,
  feedUrl = "https://pasted.example.com/feed",
  itunesId = null,
  itunesArtistId = null,
  itunesPageUrl = null,
  releaseDateIso = null,
  language = null,
  genres = emptyList(),
  explicit = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AddPodcastPresenterTest {

  @BeforeTest
  fun setUpMain() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDownMain() {
    Dispatchers.resetMain()
  }

  private fun makePresenter(
    pods: FakePodcastsRepository = FakePodcastsRepository(),
    libs: FakeLibraryRepository = FakeLibraryRepository(
      addPodcastContextResult = Result.success(
        AddPodcastContext(folders = listOf(LibraryFolder("fol", "/")), searchRegion = "us"),
      ),
    ),
    users: FakeUserRepository = FakeUserRepository(),
    navigator: FakeNavigator = FakeNavigator(SCREEN),
  ) = AddPodcastPresenter(
    screen = SCREEN,
    navigator = navigator,
    analytics = FakeAnalytics(),
    podcastsRepository = pods,
    libraryRepository = libs,
    userRepository = users,
  )

  @Test
  fun initialState_isIdle() = runTest {
    val presenter = makePresenter()
    presenter.test {
      val state = awaitItem()
      assertThat(state.searchState).isEqualTo(SearchState.Idle)
      assertThat(state.canAddPodcasts).isEqualTo(true)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun nonAdminUser_canAddPodcastsIsFalse() = runTest {
    val users = FakeUserRepository().apply {
      currentStatefulUserFlow.value = user("non_admin").copy(type = User.Type.Guest)
    }
    val presenter = makePresenter(users = users)
    presenter.test {
      var state = awaitItem()
      while (state.canAddPodcasts) {
        state = awaitItem()
      }
      assertThat(state.canAddPodcasts).isEqualTo(false)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun textQuery_callsSearchAfterDebounce() = runTest {
    val pods = FakePodcastsRepository(searchResult = Result.success(listOf(SAMPLE_RESULT)))
    val presenter = makePresenter(pods = pods)
    presenter.test {
      val initial = awaitItem()
      initial.textFieldState.edit { replace(0, length, "ozark") }
      advanceTimeBy(400.milliseconds)

      val terminal = expectMostRecentItem()
      val results = terminal.searchState
      check(results is SearchState.Results)
      assertThat(results.hits).containsExactly(SAMPLE_RESULT)
    }
    assertThat(pods.searchCalls).hasSize(1)
    assertThat(pods.searchCalls.first().query).isEqualTo("ozark")
    assertThat(pods.searchCalls.first().country).isEqualTo("us")
  }

  @Test
  fun urlInput_callsFeedAndEmitsPreview() = runTest {
    val details = PodcastFeedDetails(draft = SAMPLE_DRAFT, episodes = emptyList())
    val pods = FakePodcastsRepository(feedDetailsResult = Result.success(details))
    val presenter = makePresenter(pods = pods)
    presenter.test {
      val initial = awaitItem()
      initial.textFieldState.edit { replace(0, length, "https://example.com/feed.xml") }
      advanceTimeBy(400.milliseconds)

      val terminal = expectMostRecentItem()
      val preview = terminal.searchState
      check(preview is SearchState.FeedPreview)
      assertThat(preview.draft).isEqualTo(SAMPLE_DRAFT)
    }
    assertThat(pods.feedCalls).containsExactly("https://example.com/feed.xml")
  }

  @Test
  fun urlInput_nonAdmin_emitsForbidden() = runTest {
    val users = FakeUserRepository().apply {
      currentStatefulUserFlow.value = user("non_admin").copy(type = User.Type.Guest)
    }
    val details = PodcastFeedDetails(draft = SAMPLE_DRAFT, episodes = emptyList())
    val pods = FakePodcastsRepository(feedDetailsResult = Result.success(details))
    val presenter = makePresenter(pods = pods, users = users)
    presenter.test {
      val initial = awaitItem()
      initial.textFieldState.edit { replace(0, length, "https://example.com/feed.xml") }
      advanceTimeBy(400.milliseconds)

      val terminal = expectMostRecentItem()
      assertThat(terminal.searchState).isEqualTo(SearchState.Forbidden)
    }
    assertThat(pods.feedCalls).hasSize(0)
  }

  @Test
  fun resultTapped_navigatesImmediatelyWithoutFetching() = runTest {
    val pods = FakePodcastsRepository()
    val navigator = FakeNavigator(SCREEN)
    val presenter = makePresenter(pods = pods, navigator = navigator)
    presenter.test {
      val state = awaitItem()
      state.eventSink(AddPodcastUiEvent.ResultTapped(SAMPLE_RESULT))
      val next = navigator.awaitNextScreen()
      check(next is AddPodcastBuilderScreen)
      assertThat(next.libraryId).isEqualTo(LIB)
      assertThat(next.draft.feedUrl).isEqualTo(SAMPLE_RESULT.feedUrl)
      assertThat(next.draft.itunesId).isEqualTo(SAMPLE_RESULT.itunesId)
      cancelAndIgnoreRemainingEvents()
    }
    // The search screen no longer fetches the feed on tap — that's the builder's job now.
    assertThat(pods.feedCalls).hasSize(0)
  }
}
