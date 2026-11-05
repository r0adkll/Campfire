// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.actions

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.test.user
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.User
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.RescanResult
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.user.test.FakeUserRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.LocalOverlayHost
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

  private val actions = DefaultLibraryItemActions(
    userRepository = userRepository,
    libraryItemRepository = libraryItemRepository,
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
  fun anAccountThatCantActIsShownNothing() = runComposeUiTest {
    signIn(User.Type.User, delete = false, update = false)
    showActions()
    waitForIdle()

    onNodeWithTag("item_action_rescan").assertDoesNotExist()
    onNodeWithTag("item_action_delete").assertDoesNotExist()
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

  private fun ComposeUiTest.showActions(pointer: Offset? = null) {
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
