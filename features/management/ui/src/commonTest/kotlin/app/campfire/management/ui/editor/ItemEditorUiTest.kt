// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.management.ui.match.MatchUiState
import app.campfire.management.ui.match.matchUiState
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ItemEditorUiTest {

  private val details = mediaMetadata(title = "Children of Ruin", subtitle = null, publisher = "Pan Macmillan")
  private val book = libraryItem(id = "book", media = media(metadata = details))
  private val events = TestEventSink<ItemEditorUiEvent>()
  private var draft: BookDetailsDraft? = null

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
    onNodeWithTag("field_explicit").performScrollTo().performClick()

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

    val title = onNodeWithTag("field_title").getUnclippedBoundsInRoot()
    val publisher = onNodeWithTag("field_publisher").getUnclippedBoundsInRoot()
    assertThat(publisher.top).isGreaterThan(title.bottom)
  }

  @Test
  fun aBookThatFailedToLoadSaysSo() = runComposeUiTest {
    setEditor(item = LoadState.Error, withDraft = false)

    onNodeWithText("Couldn't load this book's details").assertExists()
    events.assertNoEvents()
  }

  @Test
  fun aBookThatCouldntBeFetchedOffersToTryAgain() = runComposeUiTest {
    setEditor(withDraft = false, isLoadFailed = true)

    onNodeWithTag("editor_retry").performClick()

    events.assertEvents(ItemEditorUiEvent.RetryLoad)
  }

  @Test
  fun aBookStillBeingFetchedShowsItsLoading() = runComposeUiTest {
    setEditor(withDraft = false)

    onNodeWithText("Couldn't load this book's details").assertDoesNotExist()
    onNodeWithTag("field_title").assertDoesNotExist()
  }

  @Test
  fun typingAnAuthorSuggestsTheLibrarysOwn() = runComposeUiTest {
    setEditor(suggestions = DetailSuggestions(authors = listOf("Adrian Tchaikovsky", "Ann Leckie")))

    onNodeWithTag("field_authors").performTouchInput { click(centerRight - Offset(24f, 0f)) }
    onNodeWithTag("field_authors").performTextInput("leck")
    onNodeWithText("Ann Leckie").performClick()

    onNodeWithText("Ann Leckie").assertExists()
    assertThat(draft!!.authors.tokens).containsExactly("Dr. Fakenstein", "Ann Leckie")
    onNodeWithTag("editor_save").assertIsEnabled()
  }

  @Test
  fun typingAPublisherSuggestsTheLibrarysOwn() = runComposeUiTest {
    setEditor(suggestions = DetailSuggestions(publishers = listOf("Recorded Books", "Tor Audio")))

    onNodeWithTag("field_publisher").performScrollTo().performClick()
    onNodeWithTag("field_publisher").performTextReplacement("reco")
    onNodeWithText("Recorded Books").performClick()

    assertThat(draft!!.publisher.text.toString()).isEqualTo("Recorded Books")
  }

  @Test
  fun aNameTheLibraryDoesntHaveCanBeAdded() = runComposeUiTest {
    setEditor(suggestions = DetailSuggestions(genres = listOf("Space Opera")))

    // Where the typing goes, past the chips
    onNodeWithTag("field_genres").performScrollTo().performTouchInput { click(centerRight - Offset(24f, 0f)) }
    onNodeWithTag("field_genres").performTextInput("first contact")
    onNodeWithText("Add “first contact”").performClick()

    assertThat(draft!!.genres.tokens).containsExactly("Fantasy", "Testing", "first contact")
    // Only the label's left, with nothing typed
    onNodeWithTag("field_genres").assertTextEquals("Genres", "")
  }

  @Test
  fun doneAddsWhatsTyped() = runComposeUiTest {
    setEditor()

    onNodeWithTag("field_tags").performScrollTo().performTouchInput { click(centerRight - Offset(24f, 0f)) }
    onNodeWithTag("field_tags").performTextInput("Book club")
    onNodeWithTag("field_tags").performImeAction()

    assertThat(draft!!.tags.tokens).containsExactly("tag1", "tag2", "Book club")
  }

  @Test
  fun enterAddsWhatsTyped() = runComposeUiTest {
    setEditor()

    onNodeWithTag("field_tags").performScrollTo().performTouchInput { click(centerRight - Offset(24f, 0f)) }
    onNodeWithTag("field_tags").performTextInput("Book club")
    onNodeWithTag("field_tags").performKeyInput { pressKey(Key.Enter) }

    assertThat(draft!!.tags.tokens).containsExactly("tag1", "tag2", "Book club")
  }

  @Test
  fun escapeClosesTheSuggestionsBeforeTheEditor() = runComposeUiTest {
    setEditor(suggestions = DetailSuggestions(authors = listOf("Ann Leckie")))

    onNodeWithTag("field_authors").performTouchInput { click(centerRight - Offset(24f, 0f)) }
    onNodeWithTag("field_authors").performTextInput("leck")
    onNodeWithText("Ann Leckie").assertExists()

    onNodeWithTag("field_authors").performKeyInput { pressKey(Key.Escape) }
    onNodeWithText("Ann Leckie").assertDoesNotExist()
    events.assertNoEvents()

    onNodeWithTag("field_authors").performKeyInput { pressKey(Key.Escape) }
    events.assertEvents(ItemEditorUiEvent.Close)
  }

  @Test
  fun saveShortcutWorksFromAField() = runComposeUiTest {
    setEditor()

    onNodeWithTag("field_title").performTextReplacement("Children of Memory")
    onNodeWithTag("field_title").performKeyInput {
      withKeyDown(Key.CtrlLeft) { pressKey(Key.S) }
    }

    events.assertEvents(ItemEditorUiEvent.Save)
  }

  @Test
  fun aShortWindowSuggestsInARowUnderTheField() = runComposeUiTest {
    setEditor(
      suggestions = DetailSuggestions(authors = listOf("Ann Leckie")),
      height = 400.dp,
    )

    onNodeWithTag("field_authors").performScrollTo().performTouchInput { click(centerRight - Offset(24f, 0f)) }
    onNodeWithTag("field_authors").performTextInput("leck")
    // Past the wait for the keyboard to settle, and the scroll that brings the field and its row into view
    mainClock.advanceTimeBy(1_000)

    val field = onNodeWithTag("field_authors").getUnclippedBoundsInRoot()
    val suggestion = onNodeWithText("Ann Leckie").getUnclippedBoundsInRoot()
    assertThat(suggestion.top).isGreaterThan(field.bottom)
    onNodeWithText("Ann Leckie").performClick()
    assertThat(draft!!.authors.tokens).containsExactly("Dr. Fakenstein", "Ann Leckie")
  }

  @Test
  fun tappingANameTakesItOut() = runComposeUiTest {
    setEditor()

    onNodeWithText("Voice Actor Prime").performClick()

    onNodeWithText("Voice Actor Prime").assertDoesNotExist()
    onNodeWithTag("editor_save").assertIsEnabled()
  }

  @Test
  fun aSeriesNeedsANameToBeSaved() = runComposeUiTest {
    setEditor()

    onNodeWithTag("series_add").performScrollTo().performClick()
    onNodeWithTag("series_sequence_1").performTextInput("3")

    onNodeWithText("Name the series, or remove it").assertExists()
    onNodeWithTag("editor_save").assertIsNotEnabled()

    onNodeWithTag("series_remove_1").performClick()

    onNodeWithTag("series_name_1").assertDoesNotExist()
    onNodeWithTag("editor_save").assertIsNotEnabled()
  }

  @Test
  fun aSeriesRowShowsTheBooksNumber() = runComposeUiTest {
    setEditor()

    onNodeWithTag("series_name_0").assertTextEquals("Series", "Test Series")
    onNodeWithTag("series_sequence_0").assertTextEquals("Number", "0")
  }

  @Test
  fun theTabsSwitchToMatching() = runComposeUiTest {
    setEditor()

    onNodeWithTag("tab_Match").performClick()

    events.assertEvents(ItemEditorUiEvent.ShowSection(EditorSection.Match))
  }

  @Test
  fun anAccountThatCantChangeItemsHasNoTabs() = runComposeUiTest {
    setEditor(canEdit = false)

    onNodeWithTag("tab_Match").assertDoesNotExist()
  }

  @Test
  fun theMatchSectionTakesTheFormsPlace() = runComposeUiTest {
    setEditor(section = EditorSection.Match)

    onNodeWithTag("match_title").assertExists()
    onNodeWithTag("field_title").assertDoesNotExist()
  }

  @Test
  fun whereAMatchFilledTheFormFromCanBeDismissed() = runComposeUiTest {
    setEditor(filledFrom = "Audible.com")

    onNodeWithText("Filled in from Audible.com. Look over the details and save.").assertExists()
    onNodeWithTag("editor_dismiss_filled").performClick()

    events.assertEvents(ItemEditorUiEvent.DismissFilledFrom)
  }

  @Test
  fun aMatchsNewCoverCanBeLeftOut() = runComposeUiTest {
    setEditor(newCoverUrl = "https://m.media-amazon.com/images/I/cover.jpg")

    onNodeWithTag("editor_new_cover").assertExists()
    onNodeWithTag("editor_keep_cover").performClick()

    onNodeWithTag("editor_new_cover").assertDoesNotExist()
    assertThat(draft!!.newCoverUrl).isEqualTo(null)
  }

  private fun ComposeUiTest.setEditor(
    item: LoadState<out LibraryItem> = LoadState.Loaded(book),
    withDraft: Boolean = true,
    canEdit: Boolean = true,
    canSeeExplicit: Boolean = true,
    isBehindServer: Boolean = false,
    isConfirmingDiscard: Boolean = false,
    isLoadFailed: Boolean = false,
    suggestions: DetailSuggestions = DetailSuggestions(),
    section: EditorSection = EditorSection.Details,
    match: MatchUiState = matchUiState(),
    filledFrom: String? = null,
    newCoverUrl: String? = null,
    width: Dp = 400.dp,
    height: Dp = 1600.dp,
  ) {
    draft = if (withDraft) BookDetailsDraft(details, book.media.tags).apply { this.newCoverUrl = newCoverUrl } else null
    setContent {
      ContentWithOverlays {
        ItemEditorUi(
          state = ItemEditorUiState(
            item = item,
            draft = draft,
            section = section,
            match = match,
            filledFrom = filledFrom,
            canEdit = canEdit,
            canSeeExplicit = canSeeExplicit,
            suggestions = suggestions,
            isSaving = false,
            isBehindServer = isBehindServer,
            isConfirmingDiscard = isConfirmingDiscard,
            isLoadFailed = isLoadFailed,
            eventSink = events,
          ),
          modifier = Modifier.size(width = width, height = height),
        )
      }
    }
  }
}
