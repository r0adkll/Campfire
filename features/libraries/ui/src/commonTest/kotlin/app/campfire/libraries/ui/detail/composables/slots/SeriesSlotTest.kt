// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.core.model.SeriesSequence
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class SeriesSlotTest {

  @Test
  fun seriesContentTest() = runComposeUiTest {
    val libraryItem = libraryItem {
      media {
        metadata {
          seriesSequence = SeriesSequence(
            id = "",
            name = "test_series_name",
            sequence = 0,
          )
        }
      }
    }
    val slot = SeriesSlot(
      libraryItem = libraryItem,
      series = listOf(
        SeriesWithBooks(
          series = SeriesSequence(id = "", name = "test_series_name", sequence = 0),
          books = listOf(libraryItem()),
        ),
      ),
    )

    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) {}
      }
    }

    onNodeWithText("Series").isDisplayed()
    onNodeWithText("test_series_name").isDisplayed()
  }

  @Test
  fun multipleSeriesContentTest() = runComposeUiTest {
    val firstSeries = SeriesSequence(id = "series_1", name = "test_series_one", sequence = 1)
    val secondSeries = SeriesSequence(id = "series_2", name = "test_series_two", sequence = 3)
    val libraryItem = libraryItem {
      media {
        metadata {
          seriesSequence = firstSeries
        }
      }
    }
    val slot = SeriesSlot(
      libraryItem = libraryItem,
      series = listOf(
        SeriesWithBooks(firstSeries, listOf(libraryItem())),
        SeriesWithBooks(secondSeries, listOf(libraryItem())),
      ),
    )

    val events = mutableListOf<LibraryItemUiEvent>()
    setContent {
      PreviewSharedElementTransitionLayout {
        slot.Content(Modifier) { events += it }
      }
    }

    onNodeWithText("test_series_one").assertIsDisplayed()
    onNodeWithText("test_series_two").assertIsDisplayed()

    onNodeWithText("test_series_two").performClick()
    assertEquals(
      listOf<LibraryItemUiEvent>(LibraryItemUiEvent.SeriesClick(libraryItem, secondSeries)),
      events,
    )
  }
}
