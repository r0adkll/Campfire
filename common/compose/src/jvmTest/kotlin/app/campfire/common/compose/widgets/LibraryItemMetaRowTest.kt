// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.campfire.core.model.preview.libraryItem
import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class LibraryItemMetaRowTest {

  @Test
  fun `each fact renders its label`() = runComposeUiTest {
    setContent {
      LibraryItemMetaRow(
        meta = listOf(
          LibraryItemMeta.Length(4.hours + 5.minutes),
          LibraryItemMeta.Remaining(30.minutes),
          LibraryItemMeta.Size(2L * 1024L * 1024L),
          LibraryItemMeta.Episodes(1),
          LibraryItemMeta.Tracks(12),
          LibraryItemMeta.PublishedYear("2019"),
          LibraryItemMeta.SeriesPosition("2.5"),
          LibraryItemMeta.AddedAt(0L),
          LibraryItemMeta.FinishedAt(0L),
        ),
      )
    }

    onNodeWithText("4h 5m").assertExists()
    onNodeWithText("30m left").assertExists()
    onNodeWithText("2.0 MB").assertExists()
    onNodeWithText("1 episode").assertExists()
    onNodeWithText("12 tracks").assertExists()
    onNodeWithText("2019").assertExists()
    onNodeWithText("Book 2.5").assertExists()
  }

  @Test
  fun `an ebook row shows its size instead of a zero length`() = runComposeUiTest {
    setContent {
      PreviewSharedElementTransitionLayout {
        LibraryItemListItem(
          libraryItem = libraryItem(duration = 0.hours, numTracks = 0, ebookFormat = "epub"),
          onClick = {},
        )
      }
    }

    onNodeWithText("1.0 MB").assertExists()
    onNodeWithText("0s").assertDoesNotExist()
  }

  @Test
  fun `a line too long for its space scrolls instead of squeezing later facts`() = runComposeUiTest {
    setContent {
      LibraryItemMetaRow(
        meta = listOf(
          LibraryItemMeta.PublishedYear("Yesterday afternoon"),
          LibraryItemMeta.SeriesPosition("5"),
        ),
        modifier = Modifier.width(80.dp),
      )
    }

    assertThat(onNodeWithText("Book 5").fetchSemanticsNode().layoutInfo.width).isGreaterThan(MinLabelWidthPx)
  }
}

// "Book 5" squeezed down to an ellipsis measured ~6px on device
private const val MinLabelWidthPx = 20
