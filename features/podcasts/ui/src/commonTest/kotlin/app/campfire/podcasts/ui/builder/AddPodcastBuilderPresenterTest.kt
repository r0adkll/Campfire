// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.builder

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryFolder
import app.campfire.podcasts.api.AddPodcastException
import app.campfire.podcasts.api.PodcastDraft
import app.campfire.podcasts.api.PodcastFeedDetails
import app.campfire.podcasts.api.RemotePodcastEpisode
import app.campfire.podcasts.api.screen.AddPodcastBuilderScreen
import app.campfire.podcasts.ui.FakeLibraryRepository
import app.campfire.podcasts.ui.FakePodcastsRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private const val LIB = "lib_1"
private val DRAFT = PodcastDraft(
  title = "My Podcast",
  author = "Author",
  descriptionHtml = null,
  descriptionPlain = "About my podcast",
  coverUrl = null,
  feedUrl = "https://example.com/feed.xml",
  itunesId = "123",
  itunesArtistId = null,
  itunesPageUrl = null,
  releaseDateIso = null,
  language = null,
  genres = emptyList(),
  explicit = false,
)
private val FOLDER_A = LibraryFolder(id = "fol_a", fullPath = "/data/podcasts")
private val FOLDER_B = LibraryFolder(id = "fol_b", fullPath = "/data/podcasts-archive")
private val SCREEN = AddPodcastBuilderScreen(libraryId = LIB, draft = DRAFT)

private val EPISODE_1 = RemotePodcastEpisode(
  title = "Ep 1",
  enclosureUrl = "https://example.com/ep1.mp3",
)
private val EPISODE_2 = RemotePodcastEpisode(
  title = "Ep 2",
  enclosureUrl = "https://example.com/ep2.mp3",
)

@OptIn(ExperimentalCoroutinesApi::class)
class AddPodcastBuilderPresenterTest {

  // Submit uses rememberRetainedCoroutineScope which is built on Dispatchers.Main.
  // runTest does not override Main by default, so we wire up the test dispatcher manually.
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
      addPodcastContextResult = Result.success(AddPodcastContext(listOf(FOLDER_A), null)),
    ),
    navigator: FakeNavigator = FakeNavigator(SCREEN),
  ) = AddPodcastBuilderPresenter(
    screen = SCREEN,
    navigator = navigator,
    analytics = FakeAnalytics(),
    libraryRepository = libs,
    podcastsRepository = pods,
  )

  @Test
  fun foldersLoad_autoSelectsSingleFolder() = runTest {
    val presenter = makePresenter()
    presenter.test {
      assertThat(awaitItem().foldersState).isEqualTo(FoldersState.Loading)
      val loaded = awaitItem().foldersState
      check(loaded is FoldersState.Loaded)
      assertThat(loaded.folders).containsExactly(FOLDER_A)
      assertThat(loaded.selectedId).isEqualTo(FOLDER_A.id)
    }
  }

  @Test
  fun foldersLoad_multipleFolders_selectsFirst() = runTest {
    val libs = FakeLibraryRepository(
      addPodcastContextResult = Result.success(
        AddPodcastContext(listOf(FOLDER_A, FOLDER_B), null),
      ),
    )
    val presenter = makePresenter(libs = libs)
    presenter.test {
      awaitItem() // Loading
      val state = awaitItem().foldersState
      check(state is FoldersState.Loaded)
      assertThat(state.folders).containsExactly(FOLDER_A, FOLDER_B)
      assertThat(state.selectedId).isEqualTo(FOLDER_A.id)
    }
  }

  @Test
  fun foldersFail_setsError() = runTest {
    val libs = FakeLibraryRepository(
      addPodcastContextResult = Result.failure(RuntimeException("boom")),
    )
    val presenter = makePresenter(libs = libs)
    presenter.test {
      awaitItem() // Loading
      assertThat(awaitItem().foldersState).isEqualTo(FoldersState.Error)
    }
  }

  @Test
  fun folderSelected_updatesState() = runTest {
    val libs = FakeLibraryRepository(
      addPodcastContextResult = Result.success(
        AddPodcastContext(listOf(FOLDER_A, FOLDER_B), null),
      ),
    )
    val presenter = makePresenter(libs = libs)
    presenter.test {
      awaitItem() // Loading
      val loaded = awaitItem()
      loaded.eventSink(AddPodcastBuilderUiEvent.FolderSelected(FOLDER_B.id))
      val state = awaitItem().foldersState
      check(state is FoldersState.Loaded)
      assertThat(state.selectedId).isEqualTo(FOLDER_B.id)
    }
  }

  @Test
  fun submit_success_popsTwiceAndCallsRepo() = runTest {
    val pods = FakePodcastsRepository(addResult = Result.success("new_item"))
    val navigator = FakeNavigator(SCREEN)
    val presenter = makePresenter(pods = pods, navigator = navigator)
    presenter.test {
      awaitItem() // Loading
      val state = awaitItem() // Loaded
      state.eventSink(AddPodcastBuilderUiEvent.Submit)
      advanceUntilIdle()

      navigator.awaitPop()
      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(pods.addCalls).hasSize(1)
    assertThat(pods.addCalls.first().libraryId).isEqualTo(LIB)
    assertThat(pods.addCalls.first().folder).isEqualTo(FOLDER_A)
    assertThat(pods.addCalls.first().autoDownloadEpisodes).isEqualTo(true)
  }

  @Test
  fun submit_forbidden_surfacesError() = runTest {
    val pods = FakePodcastsRepository(
      addResult = Result.failure(AddPodcastException(AddPodcastException.Kind.Forbidden)),
    )
    val presenter = makePresenter(pods = pods)
    presenter.test {
      awaitItem() // Loading
      val state = awaitItem()
      assertThat(state.submitError).isNull()
      state.eventSink(AddPodcastBuilderUiEvent.Submit)
      advanceUntilIdle()

      val terminal = expectMostRecentItem()
      assertThat(terminal.submitError).isEqualTo(SubmitError.Forbidden)
      assertThat(terminal.isSubmitting).isEqualTo(false)
    }
  }

  @Test
  fun submit_pathConflict_surfacesError() = runTest {
    val pods = FakePodcastsRepository(
      addResult = Result.failure(AddPodcastException(AddPodcastException.Kind.PathConflict)),
    )
    val presenter = makePresenter(pods = pods)
    presenter.test {
      awaitItem() // Loading
      awaitItem().eventSink(AddPodcastBuilderUiEvent.Submit)
      advanceUntilIdle()

      val terminal = expectMostRecentItem()
      assertThat(terminal.submitError).isEqualTo(SubmitError.PathConflict)
    }
  }

  @Test
  fun autoDownloadToggle_flipsState() = runTest {
    val presenter = makePresenter()
    presenter.test {
      awaitItem() // Loading
      val initial = awaitItem()
      assertThat(initial.autoDownloadEnabled).isEqualTo(true)
      initial.eventSink(AddPodcastBuilderUiEvent.AutoDownloadToggled(false))
      assertThat(awaitItem().autoDownloadEnabled).isEqualTo(false)
    }
  }

  @Test
  fun feedLoads_hydratesBlankDescription() = runTest {
    val itunesDraft = DRAFT.copy(descriptionPlain = null, descriptionHtml = null)
    val screen = AddPodcastBuilderScreen(libraryId = LIB, draft = itunesDraft)
    val feedDraft = itunesDraft.copy(descriptionPlain = "Rich feed description")
    val pods = FakePodcastsRepository(
      feedDetailsResult = Result.success(PodcastFeedDetails(feedDraft, emptyList())),
    )
    val presenter = AddPodcastBuilderPresenter(
      screen = screen,
      navigator = FakeNavigator(screen),
      analytics = FakeAnalytics(),
      libraryRepository = FakeLibraryRepository(
        addPodcastContextResult = Result.success(AddPodcastContext(listOf(FOLDER_A), null)),
      ),
      podcastsRepository = pods,
    )

    presenter.test {
      // Multiple intermediate emissions (folders + feed); drain until the description is filled.
      var state = awaitItem()
      while (state.descriptionState.text.isBlank()) {
        state = awaitItem()
      }
      assertThat(state.descriptionState.text.toString()).isEqualTo("Rich feed description")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun feedLoads_doesNotOverrideTypedDescription() = runTest {
    val itunesDraft = DRAFT.copy(descriptionPlain = null, descriptionHtml = null)
    val screen = AddPodcastBuilderScreen(libraryId = LIB, draft = itunesDraft)
    val pods = FakePodcastsRepository(
      feedDetailsResult = Result.success(
        PodcastFeedDetails(
          itunesDraft.copy(descriptionPlain = "Feed description"),
          emptyList(),
        ),
      ),
    )
    // Stall the feed fetch so the test can type into the description first.
    val libs = FakeLibraryRepository(
      addPodcastContextResult = Result.success(AddPodcastContext(listOf(FOLDER_A), null)),
    )
    val presenter = AddPodcastBuilderPresenter(
      screen = screen,
      navigator = FakeNavigator(screen),
      analytics = FakeAnalytics(),
      libraryRepository = libs,
      podcastsRepository = pods,
    )

    presenter.test {
      val initial = awaitItem()
      // Type something before the feed has a chance to land.
      initial.descriptionState.edit { replace(0, length, "user typed this") }
      // Drain until things settle.
      var state = expectMostRecentItem()
      // The hydration check is "isBlank() → fill", so user input must be preserved.
      assertThat(state.descriptionState.text.toString()).isEqualTo("user typed this")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun episodeToggle_updatesSelectedSet() = runTest {
    val pods = FakePodcastsRepository(
      feedDetailsResult = Result.success(
        PodcastFeedDetails(DRAFT, listOf(EPISODE_1, EPISODE_2)),
      ),
    )
    val presenter = makePresenter(pods = pods)
    presenter.test {
      var state = awaitItem()
      while (state.feedState !is FeedState.Loaded) {
        state = awaitItem()
      }
      state.eventSink(
        AddPodcastBuilderUiEvent.EpisodeSelectionToggled(EPISODE_1.enclosureUrl),
      )
      val afterToggle = awaitItem()
      assertThat(afterToggle.selectedEpisodeUrls).isEqualTo(setOf(EPISODE_1.enclosureUrl))

      afterToggle.eventSink(AddPodcastBuilderUiEvent.SelectAllEpisodes)
      val afterAll = awaitItem()
      assertThat(afterAll.selectedEpisodeUrls)
        .isEqualTo(setOf(EPISODE_1.enclosureUrl, EPISODE_2.enclosureUrl))

      afterAll.eventSink(AddPodcastBuilderUiEvent.ClearEpisodeSelection)
      assertThat(awaitItem().selectedEpisodeUrls).isEqualTo(emptySet())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun submit_withSelectedEpisodes_queuesDownloads() = runTest {
    val pods = FakePodcastsRepository(
      feedDetailsResult = Result.success(
        PodcastFeedDetails(DRAFT, listOf(EPISODE_1, EPISODE_2)),
      ),
      addResult = Result.success("new_item"),
    )
    val navigator = FakeNavigator(SCREEN)
    val presenter = makePresenter(pods = pods, navigator = navigator)
    presenter.test {
      var state = awaitItem()
      while (state.feedState !is FeedState.Loaded) {
        state = awaitItem()
      }
      state.eventSink(
        AddPodcastBuilderUiEvent.EpisodeSelectionToggled(EPISODE_1.enclosureUrl),
      )
      val selected = awaitItem()
      selected.eventSink(AddPodcastBuilderUiEvent.Submit)
      advanceUntilIdle()

      navigator.awaitPop()
      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(pods.addCalls).hasSize(1)
    assertThat(pods.queueDownloadsCalls).hasSize(1)
    assertThat(pods.queueDownloadsCalls.first().libraryItemId).isEqualTo("new_item")
    assertThat(pods.queueDownloadsCalls.first().episodes).containsExactly(EPISODE_1)
  }

  @Test
  fun submit_withoutSelectedEpisodes_skipsQueue() = runTest {
    val pods = FakePodcastsRepository(
      feedDetailsResult = Result.success(
        PodcastFeedDetails(DRAFT, listOf(EPISODE_1, EPISODE_2)),
      ),
      addResult = Result.success("new_item"),
    )
    val navigator = FakeNavigator(SCREEN)
    val presenter = makePresenter(pods = pods, navigator = navigator)
    presenter.test {
      var state = awaitItem()
      while (state.feedState !is FeedState.Loaded) {
        state = awaitItem()
      }
      state.eventSink(AddPodcastBuilderUiEvent.Submit)
      advanceUntilIdle()

      navigator.awaitPop()
      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }

    assertThat(pods.queueDownloadsCalls).hasSize(0)
  }
}
