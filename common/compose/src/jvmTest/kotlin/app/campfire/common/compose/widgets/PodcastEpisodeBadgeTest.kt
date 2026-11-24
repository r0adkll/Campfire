// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class PodcastEpisodeBadgeTest {

  @Test
  fun `a numbered episode badges its number`() = runComposeUiTest {
    setContent {
      Cover { LibraryItemCard(item = libraryItem(), episode = episode(number = "214")) }
    }

    onNodeWithText("Episode 214", useUnmergedTree = true).assertExists()
  }

  @Test
  fun `an unnumbered episode still badges as an episode`() = runComposeUiTest {
    setContent {
      Cover { LibraryItemCard(item = libraryItem(), episode = episode(number = null)) }
    }

    onNodeWithText("Episode", useUnmergedTree = true).assertExists()
  }

  @Test
  fun `a blank episode number badges as an unnumbered episode`() = runComposeUiTest {
    setContent {
      Cover { LibraryItemCard(item = libraryItem(), episode = episode(number = " ")) }
    }

    onNodeWithText("Episode", useUnmergedTree = true).assertExists()
  }

  @Test
  fun `the dense grid's compact badges compose`() = runComposeUiTest {
    setContent {
      Cover {
        LibraryItemCard(item = libraryItem(), episode = episode(number = "7"), showInformation = false)
      }
    }

    onNodeWithText("Episode 7", useUnmergedTree = true).assertExists()
  }

  private fun episode(number: String?) = PodcastEpisode(
    id = "episode",
    libraryItemId = "preview_item_id",
    podcastId = "podcast",
    episode = number,
    title = "Stories Told Around the Fire",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = 0L,
  )

  @Composable
  private fun Cover(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentLayout provides ContentLayout.Root) {
      PreviewSharedElementTransitionLayout {
        content()
      }
    }
  }
}
