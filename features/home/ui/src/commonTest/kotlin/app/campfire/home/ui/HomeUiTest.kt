// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.ShelfEntity
import app.campfire.home.api.FeedResponse
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@OptIn(ExperimentalTestApi::class)
class HomeUiTest {

  private val events = TestEventSink<HomeUiEvent>()

  @Test
  fun pullingDownTheFeedRefreshesIt() = runComposeUiTest {
    setContent {
      // Phone-sized, so the insets resolve the way they do on a handset
      CompositionLocalProvider(
        LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800),
        LocalContentLayout provides ContentLayout.Root,
      ) {
        HomeScreen(
          state = HomeUiState(
            homeFeed = FeedResponse.Success(
              persistentListOf(
                UiShelf<ShelfEntity>("one", "Shelf 1", total = 5, entities = LoadState.Loaded(emptyList())),
                UiShelf<ShelfEntity>("two", "Shelf 2", total = 3, entities = LoadState.Loaded(emptyList())),
              ),
            ),
            offlineStates = persistentMapOf(),
            progressStates = persistentMapOf(),
            isRefreshing = false,
            eventSink = events::invoke,
          ),
          campfireAppbar = { _, _ -> },
        )
      }
    }

    // The feed column, not the horizontal shelf rows inside it
    onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
      .performTouchInput { swipeDown() }
    waitForIdle()

    events.assertEvent(HomeUiEvent.Refresh)
  }
}
