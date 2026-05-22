package app.campfire.podcasts.ui.downloads

import app.campfire.core.model.User
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.screen.PodcastDownloadQueueScreen
import app.campfire.podcasts.ui.FakePodcastsRepository
import app.campfire.user.test.FakeUserRepository
import app.campfire.user.test.fixtures.user
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastDownloadQueuePresenterTest {

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `empty tracker maps to empty groups and triggers initial fetch`() = runTest {
    val deps = buildDeps()
    val presenter = deps.presenter()

    presenter.test {
      val state = expectMostRecentItem()

      assertThat(state.groups).hasSize(0)
      assertThat(state.isRefreshing).isFalse()
      assertThat(state.isAdmin).isTrue() // user() defaults to Type.Root
      assertThat(deps.repo.fetchEpisodeDownloadsCalls).containsExactly("fake_library_id")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `tracker state maps to groups sorted by podcast title`() = runTest {
    val deps = buildDeps()
    deps.tracker.setState(
      mapOf(
        "li_zebra" to listOf(remoteDownload(id = "d1", libraryItemId = "li_zebra", podcastTitle = "Zebra")),
        "li_apple" to listOf(remoteDownload(id = "d2", libraryItemId = "li_apple", podcastTitle = "Apple")),
      ),
    )
    val presenter = deps.presenter()

    presenter.test {
      val state = expectMostRecentItem()

      assertThat(state.groups.map { it.podcastTitle }).containsExactly("Apple", "Zebra")
      assertThat(state.groups.map { it.libraryItemId }).containsExactly("li_apple", "li_zebra")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `non-admin user has isAdmin false`() = runTest {
    val deps = buildDeps()
    deps.userRepo.currentStatefulUserFlow.value = user("u1").copy(type = User.Type.User)
    val presenter = deps.presenter()

    presenter.test {
      val state = expectMostRecentItem()
      assertThat(state.isAdmin).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `clear queue event invokes repository`() = runTest {
    val deps = buildDeps()
    val presenter = deps.presenter()

    presenter.test {
      expectMostRecentItem().eventSink(PodcastDownloadQueueUiEvent.ClearQueue("li_target"))
      // The UnconfinedTestDispatcher runs the launched coroutine immediately.
      assertThat(deps.repo.clearQueueCalls).containsExactly("li_target")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `open podcast event navigates to library item screen`() = runTest {
    val deps = buildDeps()
    val presenter = deps.presenter()

    presenter.test {
      expectMostRecentItem().eventSink(PodcastDownloadQueueUiEvent.OpenPodcast("li_target"))

      assertThat(deps.navigator.awaitNextScreen())
        .isEqualTo(LibraryItemScreen(libraryItemId = "li_target"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `refresh event calls fetch episode downloads again`() = runTest {
    val deps = buildDeps()
    val presenter = deps.presenter()

    presenter.test {
      // Initial fetch from LaunchedEffect = 1
      expectMostRecentItem().eventSink(PodcastDownloadQueueUiEvent.Refresh)
      // Second fetch from Refresh event
      // Allow recomposition to drain
      expectMostRecentItem()

      assertThat(deps.repo.fetchEpisodeDownloadsCalls).hasSize(2)
      cancelAndIgnoreRemainingEvents()
    }
  }

  // ---------- builders ----------

  private data class Deps(
    val repo: FakePodcastsRepository,
    val userRepo: FakeUserRepository,
    val tracker: FakeRemoteEpisodeDownloadTracker,
    val navigator: FakeNavigator,
  ) {
    fun presenter() = PodcastDownloadQueuePresenter(
      navigator = navigator,
      userRepository = userRepo,
      podcastsRepository = repo,
      tracker = tracker,
    )
  }

  private fun buildDeps(): Deps = Deps(
    repo = FakePodcastsRepository(),
    userRepo = FakeUserRepository(),
    tracker = FakeRemoteEpisodeDownloadTracker(),
    navigator = FakeNavigator(PodcastDownloadQueueScreen),
  )

  private fun remoteDownload(
    id: String,
    libraryItemId: String,
    podcastTitle: String? = null,
    state: RemoteEpisodeDownload.State = RemoteEpisodeDownload.State.Queued,
  ) = RemoteEpisodeDownload(
    id = id,
    libraryItemId = libraryItemId,
    libraryId = "lib_1",
    url = "https://feed.example.com/$id.mp3",
    episodeDisplayTitle = "Ep $id",
    podcastTitle = podcastTitle,
    state = state,
    createdAt = 0L,
    startedAt = null,
  )
}
