// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.home.ui.mediaMetadata
import app.campfire.management.api.match.BookMatch
import app.campfire.management.ui.editor.BookDetailsDraft
import app.campfire.management.ui.match.FakeMetadataMatchRepository.Companion.Audible
import app.campfire.management.ui.match.FakeMetadataMatchRepository.Companion.Google
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class MatchSectionUiTest {

  private val events = TestEventSink<MatchUiEvent>()
  private val draft = BookDetailsDraft(
    mediaMetadata(title = "Children of Ruin", subtitle = null, genres = listOf("Science Fiction")),
  )
  private val match = BookMatch(
    title = "Children of Ruin",
    subtitle = "Children of Time, Book 2",
    authors = listOf("Adrian Tchaikovsky"),
    narrators = listOf("Mel Hudson"),
    publishedYear = "2019",
    genres = listOf("Space Opera"),
    coverUrl = "https://m.media-amazon.com/images/I/cover.jpg",
    asin = "B07NDMXD3K",
    duration = 15.hours + 31.minutes,
    confidence = 0.92f,
  )

  @Test
  fun aResultShowsWhatTellsItApart() = runComposeUiTest {
    setSection(
      search = MatchSearch.Found(Audible, listOf(match)),
      bookDuration = 15.hours + 30.minutes,
      bookAsin = "b07ndmxd3k",
    )

    onNodeWithText("Same length").assertExists()
    onNodeWithText("92% match").assertExists()
    onNodeWithText("Same ASIN").assertExists()
    onNodeWithTag("match_candidate").performClick()

    events.assertEvents(MatchUiEvent.Review(match))
  }

  @Test
  fun aSearchUnderWayCantBeStartedAgain() = runComposeUiTest {
    setSection(search = MatchSearch.Searching)

    onNodeWithTag("match_searching").assertExists()
    onNodeWithTag("match_search").assertIsNotEnabled()
  }

  @Test
  fun nothingFoundSaysWhere() = runComposeUiTest {
    setSection(search = MatchSearch.Found(Audible, emptyList()))

    onNodeWithText("Nothing found on Audible.com. Try another source, or fewer words.").assertExists()
  }

  @Test
  fun aFailedSearchCanBeTriedAgain() = runComposeUiTest {
    setSection(search = MatchSearch.Failed(Audible))

    onNodeWithText("Couldn't search Audible.com. Try again in a moment.").assertExists()
    onNodeWithTag("match_retry").performClick()

    events.assertEvents(MatchUiEvent.Search)
  }

  @Test
  fun anotherSourceCanBeChosen() = runComposeUiTest {
    setSection()

    onNodeWithTag("match_source").performClick()
    onNodeWithText("Google Books").performClick()

    events.assertEvents(MatchUiEvent.ChooseProvider(Google))
  }

  @Test
  fun aReviewListsWhatWouldChangeAndHidesWhatWouldnt() = runComposeUiTest {
    setSection(review = review())

    onNodeWithTag("review_row_Subtitle").assertExists()
    onNodeWithTag("review_row_Title").assertDoesNotExist()
    onNodeWithTag("review_count").assertExists()

    scrollTo("review_unchanged").performClick()

    scrollTo("review_row_Title").assertExists()
  }

  @Test
  fun aRowTakesOrLeavesItsDetail() = runComposeUiTest {
    setSection(review = review())

    onNodeWithTag("review_row_Subtitle").performClick()
    scrollTo("review_merge_Add").performClick()

    events.assertEvents(
      MatchUiEvent.Toggle(MatchField.Subtitle),
      MatchUiEvent.Merge(MatchField.Genres, ListMerge.Add),
    )
  }

  @Test
  fun theCoverCantBeTakenWithoutTheUploadPermission() = runComposeUiTest {
    setSection(review = review(canChangeCover = false), canChangeCover = false)

    onNodeWithTag("review_row_Cover").assertIsNotEnabled()
    onNodeWithText("Changing the cover takes the upload permission").assertExists()
  }

  @Test
  fun theChosenDetailsApplyToTheFormOrSave() = runComposeUiTest {
    setSection(review = review())

    onNodeWithTag("review_apply_form").performClick()
    onNodeWithTag("review_apply_save").performClick()

    events.assertEvents(MatchUiEvent.ApplyToForm, MatchUiEvent.ApplyAndSave)
  }

  @Test
  fun nothingChosenCantBeApplied() = runComposeUiTest {
    setSection(review = review(selected = emptySet()))

    onNodeWithTag("review_apply_form").assertIsNotEnabled()
    onNodeWithTag("review_apply_save").assertIsNotEnabled()
  }

  @Test
  fun aNarrowReviewTakesTheResultsPlaceUntilClosed() = runComposeUiTest {
    setSection(search = MatchSearch.Found(Audible, listOf(match)), review = review())

    onNodeWithTag("match_results").assertDoesNotExist()
    onNodeWithTag("review_back").performClick()

    events.assertEvents(MatchUiEvent.CloseReview)
  }

  @Test
  fun aWideReviewSitsBesideTheResults() = runComposeUiTest {
    setSection(
      search = MatchSearch.Found(Audible, listOf(match)),
      review = review(),
      twoPanes = true,
      width = 1000.dp,
    )

    onNodeWithTag("match_results").assertExists()
    onNodeWithTag("match_review").assertExists()
    onNodeWithTag("review_back").assertDoesNotExist()
  }

  /** Scrolls the review to the node tagged [tag], which can be past the window's edge */
  private fun ComposeUiTest.scrollTo(tag: String): SemanticsNodeInteraction {
    onNodeWithTag("review_rows").performScrollToNode(hasTestTag(tag))
    return onNodeWithTag(tag)
  }

  private fun review(
    canChangeCover: Boolean = true,
    selected: Set<MatchField>? = null,
  ): MatchReviewState {
    val rows = reviewRows(match, draft, coverUrl = "https://abs.example/api/items/book/cover")
    return MatchReviewState(
      match = match,
      providerName = Audible.name,
      rows = rows,
      selected = selected ?: rows.selectedBy(SelectionMode.All, canChangeCover),
      merges = emptyMap(),
      canChangeCover = canChangeCover,
    )
  }

  private fun ComposeUiTest.setSection(
    search: MatchSearch = MatchSearch.NotSearched,
    review: MatchReviewState? = null,
    bookDuration: Duration? = null,
    bookAsin: String? = null,
    canChangeCover: Boolean = true,
    twoPanes: Boolean = false,
    width: Dp = 400.dp,
  ) {
    setContent {
      MatchSection(
        state = matchUiState(
          search = search,
          review = review,
          bookDuration = bookDuration,
          bookAsin = bookAsin,
          canChangeCover = canChangeCover,
          eventSink = events,
        ),
        enabled = true,
        twoPanes = twoPanes,
        modifier = Modifier.size(width = width, height = 1600.dp),
      )
    }
  }
}
