// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.core.model.preview.libraryItem
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class UnfinishedEpisodesBadgeTest {

  @Test
  fun `the badge shows the count and says what it counts`() = runComposeUiTest {
    setContent {
      UnfinishedEpisodesBadge(count = 7)
    }

    onNodeWithText("7").assertContentDescriptionEquals("7 unfinished episodes")
  }

  @Test
  fun `counts over 99 show as 99+ but are read out in full`() = runComposeUiTest {
    setContent {
      UnfinishedEpisodesBadge(count = 300)
    }

    onNodeWithText("99+").assertContentDescriptionEquals("300 unfinished episodes")
  }

  @Test
  fun `a cover badges its unfinished episodes`() = runComposeUiTest {
    setContent {
      Cover {
        LibraryItemCard(item = libraryItem(), unfinishedEpisodes = 1)
      }
    }

    onNodeWithText("1", useUnmergedTree = true).assertContentDescriptionEquals("1 unfinished episode")
  }

  @Test
  fun `a cover with every episode finished has no badge`() = runComposeUiTest {
    setContent {
      Cover {
        LibraryItemCard(item = libraryItem(), unfinishedEpisodes = 0)
      }
    }

    onNodeWithContentDescription("unfinished episode", substring = true, useUnmergedTree = true)
      .assertDoesNotExist()
  }

  @Test
  fun `a list row badges its unfinished episodes`() = runComposeUiTest {
    setContent {
      PreviewSharedElementTransitionLayout {
        LibraryItemListItem(libraryItem = libraryItem(), onClick = {}, unfinishedEpisodes = 12)
      }
    }

    onNodeWithText("12", useUnmergedTree = true).assertContentDescriptionEquals("12 unfinished episodes")
  }

  @Composable
  private fun Cover(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentLayout provides ContentLayout.Root) {
      PreviewSharedElementTransitionLayout {
        content()
      }
    }
  }
}
