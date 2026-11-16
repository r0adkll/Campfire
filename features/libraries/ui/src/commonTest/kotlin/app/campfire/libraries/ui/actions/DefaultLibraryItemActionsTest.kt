// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.actions

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.PlaybackControllerSession
import app.campfire.audioplayer.test.history.FakePlaybackHistoryRepository
import app.campfire.collections.api.ui.AddToCollectionDialog
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.test.user
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.User
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.RescanResult
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.libraries.ui.detail.ItemFinisher
import app.campfire.libraries.ui.detail.manage.FakeQuickMatchRepository
import app.campfire.libraries.ui.detail.manage.quickMatcher
import app.campfire.management.api.match.QuickMatchResult
import app.campfire.management.api.screen.ItemEditorScreen
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.test.TestLibraryViewSettings
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.FakeUserRepository
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.test.FakeNavigator
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** What picking each of an item's actions from its card does, and what's told afterwards. */
@OptIn(ExperimentalTestApi::class)
class DefaultLibraryItemActionsTest {

  private val item = libraryItem(isFile = false)
  private val userRepository = FakeUserRepository()
  private val libraryItemRepository = FakeLibraryItemRepository()
  private val snackbarHost = SnackbarHostState()
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

  private val podcast = item.copy(
    media = Media.Podcast(
      id = "podcast",
      metadata = Media.Metadata.Podcast(
        title = "A Fake Podcast",
        author = null,
        description = null,
        releaseDate = null,
        genres = emptyList(),
        feedUrl = null,
        imageUrl = null,
        itunesPageUrl = null,
        itunesId = null,
        itunesArtistId = null,
        isExplicit = false,
        language = null,
        podcastType = null,
      ),
      coverImageUrl = "",
      coverPath = null,
      tags = emptyList(),
      sizeInBytes = 0L,
    ),
  )

  private val navigator = FakeNavigator(LibraryItemScreen("elsewhere"))

  private val matchRepository = FakeQuickMatchRepository()

  private val mediaProgressRepository = FakeMediaProgressRepository()
  private val sessionsRepository = FakeSessionsRepository()
  private val sessionQueue = FakeSessionQueue()
  private val playbackController = FakePlaybackController()
  private val downloads = FakeDownloads()
  private val libraryViewSettings = TestLibraryViewSettings()
  private val quickActions = ItemQuickActions(
    userRepository = userRepository,
    mediaProgressRepository = mediaProgressRepository,
    downloads = downloads,
    sessionQueue = sessionQueue,
    sessionsRepository = sessionsRepository,
    playbackController = playbackController,
    itemFinisher = ItemFinisher(
      playbackController = playbackController,
      sessionsRepository = sessionsRepository,
      mediaProgressRepository = mediaProgressRepository,
      playbackHistoryRepository = FakePlaybackHistoryRepository(),
    ),
    libraryViewSettings = libraryViewSettings,
    addToPlaylistDialog = AddToPlaylistDialog.NoOp,
    addToCollectionDialog = object : AddToCollectionDialog {
      @Composable
      override fun Content(item: LibraryItem, onDismiss: () -> Unit, modifier: Modifier) = Unit
    },
    listeningDatesEditor = { _, _, _, _ -> },
    coroutineScopeHolder = CoroutineScopeHolder { scope },
  )

  private val actions = DefaultLibraryItemActions(
    navigator = navigator,
    userRepository = userRepository,
    libraryItemRepository = libraryItemRepository,
    quickMatcher = quickMatcher(
      matchRepository = matchRepository,
      libraryItemRepository = libraryItemRepository,
      scope = scope,
    ),
    quickActions = quickActions,
    libraryViewSettings = libraryViewSettings,
    coroutineScopeHolder = CoroutineScopeHolder { scope },
  )

  @AfterTest
  fun tearDown() {
    scope.cancel()
  }

  @Test
  fun aLongPressShowsTheItemAndWhatCanBeDoneToIt() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    showActions()

    onNodeWithText("The Great Fake Adventure").assertExists()
    onNodeWithTag("item_action_rescan").assertExists()
    onNodeWithTag("item_action_delete").assertExists()
  }

  @Test
  fun anAdminWithTheServersDefaultsIsOfferedOnlyARescan() = runComposeUiTest {
    signIn(User.Type.Admin, delete = false)
    showActions()

    onNodeWithTag("item_action_rescan").assertExists()
    onNodeWithTag("item_action_delete").assertDoesNotExist()
  }

  @Test
  fun aRightClickShowsAMenuWithoutTheItem() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    showActions(pointer = Offset(40f, 40f))

    onNodeWithTag("item_action_rescan").assertExists()
    onNodeWithTag("item_action_delete").assertExists()
    onNodeWithText("The Great Fake Adventure").assertDoesNotExist()
  }

  @Test
  fun anAccountThatCanOnlyListenIsOfferedWhatItCanDoForItsOwnListening() = runComposeUiTest {
    signIn(User.Type.User, delete = false, update = false)
    showActions()

    onNodeWithText("The Great Fake Adventure").assertExists()
    onNodeWithTag("quick_action_play").assertExists()
    onNodeWithTag("quick_action_download").assertExists()
    onNodeWithTag("quick_action_mark_finished").assertExists()
    onNodeWithTag("quick_action_add_to_queue").assertExists()
    onNodeWithTag("quick_action_add_to_playlist").assertExists()
    onNodeWithTag("quick_action_add_to_collection").assertDoesNotExist()
    onNodeWithTag("item_action_edit").assertDoesNotExist()
    onNodeWithTag("item_action_delete").assertDoesNotExist()
  }

  @Test
  fun aPodcastAnAccountCantDoAnythingToIsShownNothing() = runComposeUiTest {
    signIn(User.Type.User, delete = false, update = false)
    showActions(item = podcast)
    waitForIdle()

    onNodeWithTag("item_actions_header").assertDoesNotExist()
  }

  @Test
  fun playingStartsTheBook() = runComposeUiTest {
    signIn(User.Type.User, delete = false, update = false)
    showActions()

    onNodeWithTag("quick_action_play").performClick()
    waitForIdle()

    assertThat((playbackController.session as PlaybackControllerSession.Started).itemId).isEqualTo(item.id)
  }

  @Test
  fun markingFinishedFinishesItThenOffersToSetTheDate() = runComposeUiTest {
    signIn(User.Type.User, delete = false, update = false)
    showActions()

    onNodeWithTag("quick_action_mark_finished").performClick()

    awaitSnackbar("Marked as finished")
    assertThat(mediaProgressRepository.invocations)
      .contains(FakeMediaProgressRepository.Invocation.MarkFinished(item.id, null))
    assertThat(snackbarHost.currentSnackbarData?.visuals?.actionLabel).isEqualTo("Set date")
  }

  @Test
  fun queueingAddsItToTheQueue() = runComposeUiTest {
    signIn(User.Type.User, delete = false, update = false)
    showActions()

    onNodeWithTag("quick_action_add_to_queue").performClick()
    waitUntil(timeoutMillis = 5_000) { sessionQueue.queue.isNotEmpty() }

    assertThat(sessionQueue.queue.single().libraryItemId).isEqualTo(item.id)
  }

  @Test
  fun downloadingWithTheConfirmationTurnedOffDownloadsStraightAway() = runComposeUiTest {
    signIn(User.Type.User, delete = false, update = false)
    libraryViewSettings.showConfirmDownload = false
    showActions()

    onNodeWithTag("quick_action_download").performClick()
    waitForIdle()

    assertThat(downloads.invocations)
      .containsExactly(FakeDownloads.Invocation.Download(listOf(DownloadContent.Audiobook(item.id))))
  }

  @Test
  fun matchingOpensTheEditorOnMatch() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    showActions()

    onNodeWithTag("item_action_match").performClick()
    waitForIdle()

    assertThat(navigator.awaitNextScreen()).isEqualTo(ItemEditorScreen(item.id, ItemEditorScreen.Section.Match))
  }

  @Test
  fun aRightClickMenuHasTheQuickActionsToo() = runComposeUiTest {
    signIn(User.Type.User, delete = false, update = false)
    showActions(pointer = Offset(40f, 40f))

    onNodeWithText("Play").assertExists()
    onNodeWithText("Mark as finished").assertExists()
  }

  @Test
  fun anAccountThatCanUpdateItemsIsOfferedToEditABook() = runComposeUiTest {
    signIn(User.Type.User, delete = false, update = true)
    showActions()

    onNodeWithTag("item_action_edit").assertExists()
    onNodeWithTag("item_action_rescan").assertDoesNotExist()
  }

  @Test
  fun editingOpensTheEditor() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    showActions()

    onNodeWithTag("item_action_edit").performClick()
    waitForIdle()

    assertThat(navigator.awaitNextScreen()).isEqualTo(ItemEditorScreen(item.id))
  }

  @Test
  fun aQuickMatchAsksFirstThenSaysHowItWent() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    matchRepository.quickMatchResult = Result.success(QuickMatchResult.NoMatch)
    showActions()

    onNodeWithTag("item_action_quick_match").performClick()
    waitForIdle()
    onNodeWithText("Fills in what The Great Fake Adventure is missing", substring = true).assertExists()
    assertThat(matchRepository.quickMatches).isEmpty()

    onNodeWithTag("quick_match_replace_cover").performClick()
    onNodeWithTag("quick_match_confirm").performClick()

    awaitSnackbar("No match for The Great Fake Adventure on Audible.com")
    assertThat(matchRepository.quickMatches).containsExactly(
      FakeQuickMatchRepository.QuickMatch(item.id, "audible", replaceDetails = false, replaceCover = true),
    )
  }

  @Test
  fun aQuickMatchBackedOutOfMatchesNothing() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    showActions()

    onNodeWithTag("item_action_quick_match").performClick()
    onNodeWithText("Cancel").performClick()
    waitForIdle()

    onNodeWithTag("quick_match_confirm").assertDoesNotExist()
    assertThat(matchRepository.quickMatches).isEmpty()
  }

  @Test
  fun aRescanIsToldHowItWent() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    libraryItemRepository.rescanResult = Result.success(RescanResult.Updated)
    showActions()

    onNodeWithTag("item_action_rescan").performClick()

    awaitSnackbar("Rescanned, and updated from its files")
    assertThat(libraryItemRepository.rescanInvocations).containsExactly(item.id)
  }

  @Test
  fun aRescanTheServerRefusesSaysToAskTheAdmin() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    libraryItemRepository.rescanResult = Result.failure(ItemChangeNotAllowedException())
    showActions()

    onNodeWithTag("item_action_rescan").performClick()

    awaitSnackbar("Your account isn't allowed to rescan this. Ask your server's admin.")
  }

  @Test
  fun aDeleteAsksFirstThenRemovesTheItemFromTheLibrary() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    showActions()

    onNodeWithTag("item_action_delete").performClick()
    waitForIdle()
    assertThat(libraryItemRepository.deleteInvocations).isEmpty()

    onNodeWithTag("delete_confirm").performClick()

    awaitSnackbar("Removed The Great Fake Adventure from your library")
    assertThat(libraryItemRepository.deleteInvocations).containsExactly(Pair(item.id, false))
    assertThat(libraryViewSettings.deleteItemFilesByDefault).isFalse()
  }

  @Test
  fun aDeleteStartsFromTheLastChoiceAndStillTakesASecondPressForFiles() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    libraryViewSettings.deleteItemFilesByDefault = true
    showActions()

    onNodeWithTag("item_action_delete").performClick()
    onNodeWithTag("delete_confirm").assertTextEquals("Delete files").performClick()
    assertThat(libraryItemRepository.deleteInvocations).isEmpty()

    onNodeWithTag("delete_confirm").assertTextEquals("Press again to delete files").performClick()

    awaitSnackbar("Deleted The Great Fake Adventure and its files")
    assertThat(libraryItemRepository.deleteInvocations).containsExactly(Pair(item.id, true))
    assertThat(libraryViewSettings.deleteItemFilesByDefault).isTrue()
  }

  @Test
  fun aDeleteBackedOutOfKeepsTheLastChoice() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    libraryViewSettings.deleteItemFilesByDefault = true
    showActions()

    onNodeWithTag("item_action_delete").performClick()
    onNodeWithTag("delete_option_remove").performClick()
    onNodeWithText("Cancel").performClick()
    waitForIdle()

    assertThat(libraryViewSettings.deleteItemFilesByDefault).isTrue()
  }

  @Test
  fun aDeleteBackedOutOfDeletesNothing() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    showActions()

    onNodeWithTag("item_action_delete").performClick()
    onNodeWithText("Cancel").performClick()
    waitForIdle()

    onNodeWithTag("delete_confirm").assertDoesNotExist()
    assertThat(libraryItemRepository.deleteInvocations).isEmpty()
  }

  @Test
  fun aDeleteThatFailsSaysSo() = runComposeUiTest {
    signIn(User.Type.Root, delete = true)
    libraryItemRepository.deleteResult = Result.failure(IllegalStateException("offline"))
    showActions()

    onNodeWithTag("item_action_delete").performClick()
    onNodeWithTag("delete_confirm").performClick()

    awaitSnackbar("Couldn't delete this. Check your connection and try again.")
  }

  private fun signIn(type: User.Type, delete: Boolean, update: Boolean = true) {
    userRepository.currentStatefulUserFlow.value = user(
      id = "user-1",
      type = type,
      permissions = User.Permissions(
        download = true,
        update = update,
        delete = delete,
        upload = false,
        accessAllLibraries = true,
        accessAllTags = true,
        accessExplicitContent = true,
      ),
    )
  }

  private fun ComposeUiTest.showActions(
    pointer: Offset? = null,
    item: LibraryItem = this@DefaultLibraryItemActionsTest.item,
  ) {
    setContent {
      CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800)) {
        ContentWithOverlays {
          val overlayHost = LocalOverlayHost.current
          LaunchedEffect(Unit) {
            actions.show(item, pointer, overlayHost, snackbarHost)
          }
        }
      }
    }
    waitForIdle()
  }

  private fun ComposeUiTest.awaitSnackbar(message: String) {
    waitUntil(timeoutMillis = 5_000) {
      snackbarHost.currentSnackbarData?.visuals?.message == message
    }
  }
}
