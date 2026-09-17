// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.collections.ui.list

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.core.coroutines.LoadState
import app.campfire.core.settings.GroupDisplayState
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CollectionsUiTest {

  private val events = TestEventSink<CollectionsUiEvent>()

  @Test
  fun pullingDownTheCollectionsRefreshesThem() = runComposeUiTest {
    setContent {
      // Phone-sized, so the insets resolve the way they do on a handset
      CompositionLocalProvider(
        LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800),
        LocalContentLayout provides ContentLayout.Root,
      ) {
        Collections(
          state = CollectionsUiState(
            collectionContentState = LoadState.Loaded(emptyList()),
            displayState = GroupDisplayState.List,
            isRefreshing = false,
            eventSink = events::invoke,
          ),
        )
      }
    }

    // Start below the top app bar, which sits over the top of the grid
    onNode(hasScrollAction()).performTouchInput { swipeDown(startY = centerY, endY = bottom) }
    waitForIdle()

    events.assertEvent(CollectionsUiEvent.Refresh)
  }
}
