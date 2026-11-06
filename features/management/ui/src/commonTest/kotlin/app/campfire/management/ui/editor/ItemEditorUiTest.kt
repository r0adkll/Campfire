// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ItemEditorUiTest {

  private val details = mediaMetadata(title = "Children of Ruin", subtitle = null, publisher = "Pan Macmillan")
  private val book = libraryItem(id = "book", media = media(metadata = details))
  private val events = TestEventSink<ItemEditorUiEvent>()

  @Test
  fun theBooksDetailsFillTheFields() = runComposeUiTest {
    setEditor()

    onNodeWithText("Children of Ruin").assertExists()
    onNodeWithText("Pan Macmillan").assertExists()
  }

  @Test
  fun saveWaitsForAChange() = runComposeUiTest {
    setEditor()

    onNodeWithTag("editor_save").assertIsNotEnabled()

    onNodeWithTag("field_title").performTextReplacement("Children of Memory")

    onNodeWithTag("editor_save").assertIsEnabled().performClick()
    onNodeWithTag("editor_change_count", useUnmergedTree = true).assertExists()
    events.assertEvents(ItemEditorUiEvent.Save)
  }

  @Test
  fun aDescriptionEditOnItsOwnCanBeSaved() = runComposeUiTest {
    setEditor()

    onNodeWithTag("field_description").performClick()
    onNodeWithTag("field_description").performTextInput(" More.")

    onNodeWithTag("editor_save").assertIsEnabled()
    onNodeWithContentDescription("Changed", useUnmergedTree = true).assertExists()
  }

  @Test
  fun aBookWithoutATitleCantBeSaved() = runComposeUiTest {
    setEditor()

    onNodeWithTag("field_title").performTextReplacement("")

    onNodeWithText("A book needs a title").assertExists()
    onNodeWithTag("editor_save").assertIsNotEnabled()
  }

  @Test
  fun closingAsksThePresenter() = runComposeUiTest {
    setEditor()

    onNodeWithTag("editor_close").performClick()

    events.assertEvents(ItemEditorUiEvent.Close)
  }

  @Test
  fun theDiscardQuestionAnswersBothWays() = runComposeUiTest {
    setEditor(isConfirmingDiscard = true)

    onNodeWithText("Discard your changes?").assertExists()
    onNodeWithTag("discard_cancel").performClick()
    onNodeWithTag("discard_confirm").performClick()

    events.assertEvents(ItemEditorUiEvent.CancelDiscard, ItemEditorUiEvent.ConfirmDiscard)
  }

  @Test
  fun markingABookExplicitWarnsAnAccountThatCantSeeThem() = runComposeUiTest {
    setEditor(canSeeExplicit = false)

    onNodeWithText("Your account can't see explicit books", substring = true).assertDoesNotExist()
    onNodeWithTag("field_explicit").performClick()

    onNodeWithText("Your account can't see explicit books", substring = true).assertExists()
  }

  @Test
  fun anAccountThatCantChangeItemsIsToldAndCantType() = runComposeUiTest {
    setEditor(canEdit = false)

    onNodeWithText("Your account isn't allowed to change items. Ask your server's admin.").assertExists()
    onNodeWithTag("field_title").assertIsNotEnabled()
  }

  @Test
  fun aServerChangeOffersBothWaysForward() = runComposeUiTest {
    setEditor(isBehindServer = true)

    onNodeWithTag("editor_keep_mine").performClick()
    onNodeWithTag("editor_reload").performClick()

    events.assertEvents(ItemEditorUiEvent.KeepChanges, ItemEditorUiEvent.ResetChanges)
  }

  @Test
  fun aWideEditorSpellsOutItsChangesAndOffersToDiscardThem() = runComposeUiTest {
    setEditor(width = 900.dp)

    onNodeWithTag("field_title").performTextReplacement("Children of Memory")

    onNodeWithText("1 change").assertExists()
    onNodeWithTag("editor_change_count", useUnmergedTree = true).assertDoesNotExist()
    onNodeWithTag("editor_discard").performClick()
    events.assertEvents(ItemEditorUiEvent.ResetChanges)
  }

  @Test
  fun aWideEditorPutsPublishingBesideTheTitle() = runComposeUiTest {
    setEditor(width = 900.dp)

    val title = onNodeWithTag("field_title").fetchSemanticsNode().boundsInRoot
    val publisher = onNodeWithTag("field_publisher").fetchSemanticsNode().boundsInRoot
    assertThat(publisher.left).isGreaterThan(title.right)
  }

  @Test
  fun aNarrowEditorStacksItsFields() = runComposeUiTest {
    setEditor(width = 400.dp)

    val title = onNodeWithTag("field_title").fetchSemanticsNode().boundsInRoot
    val publisher = onNodeWithTag("field_publisher").fetchSemanticsNode().boundsInRoot
    assertThat(publisher.top).isGreaterThan(title.bottom)
  }

  @Test
  fun aBookThatFailedToLoadSaysSo() = runComposeUiTest {
    setEditor(item = LoadState.Error, withDraft = false)

    onNodeWithText("Couldn't load this book's details").assertExists()
    events.assertNoEvents()
  }

  private fun ComposeUiTest.setEditor(
    item: LoadState<out LibraryItem> = LoadState.Loaded(book),
    withDraft: Boolean = true,
    canEdit: Boolean = true,
    canSeeExplicit: Boolean = true,
    isBehindServer: Boolean = false,
    isConfirmingDiscard: Boolean = false,
    width: Dp = 400.dp,
  ) {
    val draft = if (withDraft) BookDetailsDraft(details) else null
    setContent {
      ItemEditorUi(
        state = ItemEditorUiState(
          item = item,
          draft = draft,
          canEdit = canEdit,
          canSeeExplicit = canSeeExplicit,
          isSaving = false,
          isBehindServer = isBehindServer,
          isConfirmingDiscard = isConfirmingDiscard,
          eventSink = events,
        ),
        modifier = Modifier.size(width = width, height = 1600.dp),
      )
    }
  }
}
