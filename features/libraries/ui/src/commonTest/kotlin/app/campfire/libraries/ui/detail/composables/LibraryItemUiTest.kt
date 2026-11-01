// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.collections.api.ui.AddToCollectionDialog
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.test.user
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.User
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.ui.detail.ContentUiState
import app.campfire.libraries.ui.detail.LibraryItem
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.LibraryItemUiState
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import app.campfire.libraries.ui.detail.manage.QuickMatchChoice
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test

@OptIn(
  ExperimentalTestApi::class,
  ExperimentalSharedTransitionApi::class,
)
class LibraryItemUiTest {

  val user = user("test_user-id")
  private val events = TestEventSink<LibraryItemUiEvent>()
  private val addToCollectionDialog = object : AddToCollectionDialog {
    @Composable
    override fun Content(
      item: LibraryItem,
      onDismiss: () -> Unit,
      modifier: Modifier,
    ) {
      Box(
        modifier = modifier
          .testTag("add_to_collection_dialog"),
      )
    }
  }

  @Test
  fun testErrorList() = runComposeUiTest {
    val state = LibraryItemUiState(
      user = user,
      libraryItem = null,
      theme = null,
      swatch = null,
      contentState = LoadState.Error,
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNodeWithTag("error_list_state").assertIsDisplayed()
  }

  @Test
  fun errorStateHasNoAddToCollectionAction() = runComposeUiTest {
    // A removed or unreachable item never loads, so there's nothing to add to a collection
    val state = LibraryItemUiState(
      user = user,
      libraryItem = null,
      theme = null,
      swatch = null,
      contentState = LoadState.Error,
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNodeWithContentDescription("Add to collection").assertDoesNotExist()
  }

  @Test
  fun testLoadingList() = runComposeUiTest {
    val state = LibraryItemUiState(
      user = user,
      libraryItem = null,
      theme = null,
      swatch = null,
      contentState = LoadState.Loading,
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNodeWithTag("loading_list_state").assertIsDisplayed()
  }

  @Test
  fun testLoadedList() = runComposeUiTest {
    val state = LibraryItemUiState(
      user = user,
      libraryItem = null,
      theme = null,
      swatch = null,
      contentState = LoadState.Loaded(
        data = ContentUiState(
          slots = List(20) { TestContentSlot("slot_$it") },
          eventSink = events::invoke,
        ),
      ),
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNode(hasScrollAction())
      .onChildren()
      // Account for the spacer we add to the bottom of this list
      .assertCountEquals(21)
  }

  @Test
  fun clickingBackEmitsEvent() = runComposeUiTest {
    val state = LibraryItemUiState(
      user = user,
      libraryItem = null,
      theme = null,
      swatch = null,
      contentState = LoadState.Loaded(
        data = ContentUiState(
          slots = List(20) { TestContentSlot("slot_$it") },
          eventSink = events::invoke,
        ),
      ),
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNodeWithContentDescription("Back").performClick()
    events.assertEvent(LibraryItemUiEvent.OnBack)
  }

  @Test
  fun clickingAddToCollectionShowsDialog() = runComposeUiTest {
    val state = LibraryItemUiState(
      user = user,
      libraryItem = libraryItem(),
      theme = null,
      swatch = null,
      contentState = LoadState.Loaded(
        data = ContentUiState(
          slots = List(20) { TestContentSlot("slot_$it") },
          eventSink = events::invoke,
        ),
      ),
      eventSink = events::invoke,
    )

    setContent {
      TestLibraryItem(state)
    }

    onNodeWithContentDescription("Add to collection").performClick()
    onNodeWithTag("add_to_collection_dialog").assertExists()
  }

  @Test
  fun anAccountThatCanManageItems_rescansFromTheMenu() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(user)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_rescan").performClick()

    events.assertEvent(LibraryItemUiEvent.RescanClick)
  }

  @Test
  fun anAccountThatCanManageItems_quickMatchesFromTheMenu() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(user)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_quick_match").performClick()

    events.assertEvent(LibraryItemUiEvent.QuickMatchClick)
  }

  @Test
  fun aQuickMatch_asksWhatItMayReplace() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(user).copy(quickMatchSource = "Audible.com")) }

    onNodeWithText("from the first Audible.com result", substring = true).assertExists()
    onNodeWithTag("quick_match_replace_details").performClick()
    onNodeWithText("A replaced cover can't be undone").assertDoesNotExist()
    onNodeWithTag("quick_match_replace_cover").performClick()
    onNodeWithText("A replaced cover can't be undone").assertExists()
    onNodeWithTag("quick_match_confirm").performClick()

    events.assertEvent(LibraryItemUiEvent.QuickMatch(QuickMatchChoice(replaceDetails = true, replaceCover = true)))
  }

  @Test
  fun aQuickMatch_canBeBackedOutOf() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(user).copy(quickMatchSource = "Audible.com")) }

    onNodeWithText("Cancel").performClick()

    events.assertEvent(LibraryItemUiEvent.DismissQuickMatch)
  }

  @Test
  fun deletingFiles_takesASecondPress() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(user)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_delete").performClick()
    onNodeWithTag("delete_option_hard").performClick()
    onNodeWithTag("delete_confirm").assertTextEquals("Delete files").performClick()

    // The first press only asks again
    events.assertNoEvents()
    onNodeWithTag("delete_confirm").assertTextEquals("Press again to delete files").performClick()

    events.assertEvent(LibraryItemUiEvent.DeleteItemClick(hardDelete = true))
  }

  @Test
  fun removingFromTheLibrary_isTheDefaultAndTakesOnePress() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(user)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_delete").performClick()
    onNodeWithTag("delete_confirm").assertTextEquals("Remove").performClick()

    events.assertEvent(LibraryItemUiEvent.DeleteItemClick(hardDelete = false))
  }

  @Test
  fun anAccountThatCantManageItems_hasNoMenu() = runComposeUiTest {
    val listener = user(
      id = "listener",
      type = User.Type.User,
      permissions = user.permissions.copy(update = false, delete = false),
    )

    setContent { TestLibraryItem(loadedState(listener)) }

    onNodeWithTag("item_actions_button").assertDoesNotExist()
  }

  private fun loadedState(user: User) = LibraryItemUiState(
    user = user,
    // A folder, as a single file can't be rescanned
    libraryItem = libraryItem().copy(isFile = false),
    contentState = LoadState.Loaded(
      data = ContentUiState(
        slots = listOf(TestContentSlot("slot")),
        eventSink = events::invoke,
      ),
    ),
    eventSink = events::invoke,
  )

  @Composable
  private fun TestLibraryItem(
    state: LibraryItemUiState,
    modifier: Modifier = Modifier,
  ) {
    PreviewSharedElementTransitionLayout {
      CompositionLocalProvider(
        LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 720, minHeightDp = 1080),
        LocalContentLayout provides ContentLayout.Root,
      ) {
        LibraryItem(
          screen = LibraryItemScreen(""),
          state = state,
          addToCollectionDialog = addToCollectionDialog,
          modifier = modifier,
        )
      }
    }
  }
}

private class TestContentSlot(
  override val id: String,
) : ContentSlot {

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Box(modifier = modifier.testTag(id))
  }
}
