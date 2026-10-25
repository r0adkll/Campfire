// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.cover

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.management.api.match.ImageSize
import app.campfire.management.ui.editor.StagedCover
import app.campfire.management.ui.match.FakeMetadataMatchRepository.Companion.Audible
import app.campfire.management.ui.match.FakeMetadataMatchRepository.Companion.Google
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test

/** The Cover tab: the book's cover, where a new one comes from, and looking one over. */
@OptIn(ExperimentalTestApi::class)
class CoverSectionUiTest {

  private val events = TestEventSink<CoverUiEvent>()

  @Test
  fun theCurrentCoverSaysHowBigItIs() = runComposeUiTest {
    setSection(coverUiState(currentSize = ImageSize(640, 640)))

    onNodeWithText("Current cover").assertExists()
    onNodeWithText("640 × 640").assertExists()
    onNodeWithText("Low resolution").assertExists()
  }

  @Test
  fun aBigEnoughCoverIsntCalledLowResolution() = runComposeUiTest {
    setSection(coverUiState(currentSize = ImageSize(2400, 2400)))

    onNodeWithText("Low resolution").assertDoesNotExist()
  }

  @Test
  fun aChosenCoverShowsBesideTheCurrentOneUntilKeptBack() = runComposeUiTest {
    setSection(coverUiState(staged = StagedCover.fromUrl(AudibleCover)))

    onNodeWithTag("editor_new_cover").assertExists()
    onNodeWithTag("cover_current").assertDoesNotExist()
    onNodeWithTag("editor_keep_cover").performClick()

    events.assertEvent(CoverUiEvent.KeepCurrent)
  }

  @Test
  fun eachSourcesResultsShowUnderItsName() = runComposeUiTest {
    setSection(
      coverUiState(
        results = listOf(
          CoverResults(Audible, CoverSearch.Found(listOf(AudibleCover))),
          CoverResults(Google, CoverSearch.Failed),
        ),
        sizes = mapOf(AudibleCover to ImageSize(2400, 2400)),
      ),
    )

    onNodeWithText("Audible.com").assertExists()
    onNodeWithText("2400²").assertExists()
    onNodeWithText("Google Books").assertExists()
    onNodeWithText("Couldn't search").assertExists()
    onNodeWithTag("cover_result").performClick()

    events.assertEvent(CoverUiEvent.Preview(AudibleCover))
  }

  @Test
  fun aSourceThatFoundNothingSaysSo() = runComposeUiTest {
    setSection(coverUiState(results = listOf(CoverResults(Audible, CoverSearch.Found(emptyList())))))

    onNodeWithText("None found").assertExists()
  }

  @Test
  fun theWaysToANewCoverCanBeChosen() = runComposeUiTest {
    setSection(coverUiState())

    onNodeWithTag("cover_mode_address").performClick()
    onNodeWithTag("cover_mode_bookfiles").performClick()
    onNodeWithTag("cover_remove").performClick()

    events.assertEvents(
      CoverUiEvent.ChooseMode(CoverMode.Address),
      CoverUiEvent.ChooseMode(CoverMode.BookFiles),
      CoverUiEvent.Remove,
    )
  }

  @Test
  fun withoutTheUploadPermissionOnlyTheBooksOwnImagesCanBeUsed() = runComposeUiTest {
    setSection(coverUiState(mode = CoverMode.BookFiles, canUseAddresses = false))

    onNodeWithTag("cover_mode_search").assertIsNotEnabled()
    onNodeWithTag("cover_mode_address").assertIsNotEnabled()
    onNodeWithTag("cover_mode_bookfiles").assertIsEnabled()
    onNodeWithText("Searching for covers, or using one from an address, takes the upload permission").assertExists()
  }

  @Test
  fun removingIsOfferedOnlyWhenAllowedAndThereIsACover() = runComposeUiTest {
    var state by mutableStateOf(coverUiState(canRemove = false))
    setSection { state }

    onNodeWithTag("cover_remove").assertDoesNotExist()

    state = coverUiState(currentUrl = null)
    waitForIdle()

    onNodeWithTag("cover_remove").assertDoesNotExist()
  }

  @Test
  fun onlyAnAddressCanBeLookedAt() = runComposeUiTest {
    var state by mutableStateOf(coverUiState(mode = CoverMode.Address, address = "children of ruin"))
    setSection { state }

    onNodeWithTag("cover_address_preview").assertIsNotEnabled()

    state = coverUiState(mode = CoverMode.Address, address = "https://example.com/ruin.jpg")
    waitForIdle()
    onNodeWithTag("cover_address_preview").performClick()

    events.assertEvent(CoverUiEvent.PreviewAddress)
  }

  @Test
  fun theBooksOwnImagesSayWhichIsInUse() = runComposeUiTest {
    val folder = bookImage("folder.jpg")
    setSection(
      coverUiState(
        mode = CoverMode.BookFiles,
        bookImages = BookImages.Loaded(listOf(bookImage("cover.jpg", isCover = true), folder)),
      ),
    )

    onNodeWithText("Book files · 2").assertExists()
    onNodeWithText("In use").assertExists()
    onNodeWithText("Use").performClick()

    events.assertEvent(CoverUiEvent.UseBookImage(folder))
  }

  @Test
  fun aBookWithoutImagesSaysSo_andAFailedListingCanBeTriedAgain() = runComposeUiTest {
    var state by mutableStateOf(coverUiState(mode = CoverMode.BookFiles, bookImages = BookImages.Loaded(emptyList())))
    setSection { state }

    onNodeWithText("There are no images in the book's folder").assertExists()

    state = coverUiState(mode = CoverMode.BookFiles, bookImages = BookImages.Failed)
    waitForIdle()
    onNodeWithTag("cover_files_retry").performClick()

    events.assertEvent(CoverUiEvent.RetryBookImages)
  }

  @Test
  fun aCoverLookedAtPagesAndCanBeChosen() = runComposeUiTest {
    setContent {
      CoverPreviewContent(
        preview = CoverPreview(listOf(AudibleCover, GoogleCover), index = 0, size = ImageSize(2400, 2400)),
        currentUrl = "https://abs.example/api/items/book/cover",
        currentSize = ImageSize(640, 640),
        canUse = true,
        eventSink = events,
      )
    }

    onNodeWithText("Cover 1 of 2").assertExists()
    onNodeWithText("640 × 640").assertExists()
    onNodeWithText("2400 × 2400").assertExists()
    onNodeWithText("larger").assertExists()
    onNodeWithTag("cover_preview_previous").assertIsNotEnabled()
    onNodeWithTag("cover_preview_next").performClick()
    onNodeWithTag("cover_preview_use").performClick()

    events.assertEvents(CoverUiEvent.PageTo(1), CoverUiEvent.UsePreviewed)
  }

  @Test
  fun aCoverBeingLookedAtOpensInASheet() = runComposeUiTest {
    setSection(coverUiState(preview = CoverPreview(listOf(AudibleCover), 0)))

    onNodeWithTag("cover_preview").assertExists()
    onNodeWithTag("cover_preview_next").assertDoesNotExist()
  }

  private fun ComposeUiTest.setSection(state: CoverUiState) = setSection { state }

  private fun ComposeUiTest.setSection(state: () -> CoverUiState) {
    setContent {
      // The window the sheet a cover's looked at in reads to choose its side
      CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 900)) {
        ContentWithOverlays {
          CoverSection(
            state = state().copy(eventSink = events),
            enabled = true,
            modifier = Modifier.size(width = 400.dp, height = 1600.dp),
          )
        }
      }
    }
  }

  private companion object {
    const val AudibleCover = "https://m.media-amazon.com/images/I/ruin.jpg"
    const val GoogleCover = "https://books.google.com/books/content?id=ruin"
  }
}
