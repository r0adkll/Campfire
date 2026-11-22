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
import app.campfire.core.model.LibraryItemId
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.ui.detail.ContentUiState
import app.campfire.libraries.ui.detail.LibraryItem
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.LibraryItemUiState
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import app.campfire.libraries.ui.detail.manage.ItemAction
import app.campfire.libraries.ui.detail.manage.QuickMatchChoice
import app.campfire.sessions.api.PlaybackHistoryUi
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isLessThan
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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

  private val playbackHistoryUi = RecordingPlaybackHistoryUi()

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
    setContent { TestLibraryItem(loadedState(ItemAction.entries)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_rescan").performClick()

    events.assertEvent(LibraryItemUiEvent.RescanClick)
  }

  @Test
  fun anAccountThatCanManageItems_quickMatchesFromTheMenu() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(ItemAction.entries)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_quick_match").performClick()

    events.assertEvent(LibraryItemUiEvent.QuickMatchClick)
  }

  @Test
  fun aQuickMatch_asksWhatItMayReplace() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(ItemAction.entries).copy(quickMatchSource = "Audible.com")) }

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
    setContent { TestLibraryItem(loadedState(ItemAction.entries).copy(quickMatchSource = "Audible.com")) }

    onNodeWithText("Cancel").performClick()

    events.assertEvent(LibraryItemUiEvent.DismissQuickMatch)
  }

  @Test
  fun deletingFiles_isTheFirstDefaultAndTakesASecondPress() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(ItemAction.entries)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_delete").performClick()
    onNodeWithTag("delete_confirm").assertTextEquals("Delete files").performClick()

    // The first press only asks again
    events.assertNoEvents()
    onNodeWithTag("delete_confirm").assertTextEquals("Press again to delete files").performClick()

    events.assertEvent(LibraryItemUiEvent.DeleteItemClick(hardDelete = true))
  }

  @Test
  fun removingFromTheLibrary_whenLastChosen_startsChosenAndTakesOnePress() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(ItemAction.entries, deleteFilesByDefault = false)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_delete").performClick()
    onNodeWithTag("delete_confirm").assertTextEquals("Remove").performClick()

    events.assertEvent(LibraryItemUiEvent.DeleteItemClick(hardDelete = false))
  }

  @Test
  fun removingFromTheLibrary_canBeChosenOverTheDefault() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(ItemAction.entries)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_delete").performClick()
    onNodeWithTag("delete_option_remove").performClick()
    onNodeWithTag("delete_confirm").assertTextEquals("Remove").performClick()

    events.assertEvent(LibraryItemUiEvent.DeleteItemClick(hardDelete = false))
  }

  @Test
  fun noItemActions_meansNoMenu() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(itemActions = emptyList())) }

    onNodeWithTag("item_actions_button").assertDoesNotExist()
  }

  @Test
  fun playbackHistory_isInTheMenu_evenWithoutItemActions() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(itemActions = emptyList()).copy(showPlaybackHistory = true)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_playback_history").performClick()
    waitForIdle()

    assertThat(playbackHistoryUi.shown).containsExactly(Shown("", canJump = false))
    events.assertNoEvents()
  }

  @Test
  fun playbackHistory_whilePlaying_jumpsToWhatWasPicked() = runComposeUiTest {
    playbackHistoryUi.picks = 12.minutes
    setContent {
      TestLibraryItem(
        loadedState(ItemAction.entries).copy(showPlaybackHistory = true, isPlaying = true),
      )
    }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_playback_history").performClick()
    waitForIdle()

    assertThat(playbackHistoryUi.shown).containsExactly(Shown("", canJump = true))
    events.assertEvent(LibraryItemUiEvent.PlaybackHistoryJump(12.minutes))
  }

  @Test
  fun playbackHistory_sitsJustAboveDeleting() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(ItemAction.entries).copy(showPlaybackHistory = true)) }

    onNodeWithTag("item_actions_button").performClick()

    val top = { tag: String -> onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top }
    assertThat(top("item_action_rescan")).isLessThan(top("item_action_playback_history"))
    assertThat(top("item_action_playback_history")).isLessThan(top("item_action_delete"))
  }

  @Test
  fun playbackHistory_withoutDeleting_isLast() = runComposeUiTest {
    setContent {
      TestLibraryItem(
        loadedState(ItemAction.entries - ItemAction.Delete).copy(showPlaybackHistory = true),
      )
    }

    onNodeWithTag("item_actions_button").performClick()

    val top = { tag: String -> onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top }
    assertThat(top("item_action_rescan")).isLessThan(top("item_action_playback_history"))
  }

  @Test
  fun playbackHistory_turnedOff_isNotInTheMenu() = runComposeUiTest {
    setContent { TestLibraryItem(loadedState(ItemAction.entries)) }

    onNodeWithTag("item_actions_button").performClick()
    onNodeWithTag("item_action_playback_history").assertDoesNotExist()
  }

  private fun loadedState(
    itemActions: List<ItemAction>,
    deleteFilesByDefault: Boolean = true,
  ) = LibraryItemUiState(
    user = user,
    libraryItem = libraryItem(),
    itemActions = itemActions,
    deleteFilesByDefault = deleteFilesByDefault,
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
        LocalOverlayHost provides rememberOverlayHost(),
      ) {
        LibraryItem(
          screen = LibraryItemScreen(""),
          state = state,
          addToCollectionDialog = addToCollectionDialog,
          playbackHistoryUi = playbackHistoryUi,
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

private data class Shown(val libraryItemId: LibraryItemId, val canJump: Boolean)

private class RecordingPlaybackHistoryUi : PlaybackHistoryUi {
  val shown = mutableListOf<Shown>()
  var picks: Duration? = null

  override suspend fun showPlaybackHistory(
    overlayHost: OverlayHost,
    libraryItemId: LibraryItemId,
    canJump: Boolean,
  ): Duration? {
    shown += Shown(libraryItemId, canJump)
    return picks
  }
}
