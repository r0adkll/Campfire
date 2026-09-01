// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.discover.api.DiscoverScanResults
import app.campfire.discover.api.DiscoverScanState
import app.campfire.discover.api.DiscoverScanTracker
import app.campfire.discover.api.screen.DiscoverScreen
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest

private class FakeDiscoverScanTracker : DiscoverScanTracker {
  val stateFlow = MutableStateFlow<DiscoverScanState>(DiscoverScanState.Idle)
  override val state: StateFlow<DiscoverScanState> = stateFlow

  var startScanCount = 0
  override fun startScan() {
    startScanCount++
  }

  var startScanIfStaleCount = 0
  override fun startScanIfStale() {
    startScanIfStaleCount++
  }

  var cancelScanCount = 0
  override fun cancelScan() {
    cancelScanCount++
  }
}

class DiscoverPresenterTest {

  private val navigator = FakeNavigator(DiscoverScreen)
  private val tracker = FakeDiscoverScanTracker()
  private val presenter = DiscoverPresenter(navigator, tracker)

  @Test
  fun present_OpeningTheScreen_ScansOnlyIfStale() = runTest {
    presenter.test {
      awaitItem()

      // The freshness policy lives in the tracker — opening never forces a scan.
      assertThat(tracker.startScanIfStaleCount).isEqualTo(1)
      assertThat(tracker.startScanCount).isEqualTo(0)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_RefreshEvent_ForcesAScan() = runTest {
    presenter.test {
      val state = awaitItem()

      state.eventSink(DiscoverUiEvent.Refresh)

      assertThat(tracker.startScanCount).isEqualTo(1)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_CancelEvent_CancelsTheTracker() = runTest {
    presenter.test {
      val state = awaitItem()

      state.eventSink(DiscoverUiEvent.CancelScan)

      assertThat(tracker.cancelScanCount).isEqualTo(1)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_TrackerState_FlowsThrough() = runTest {
    presenter.test {
      awaitItem()

      val completed = DiscoverScanState.Completed(
        results = DiscoverScanResults(providerName = "Audible"),
        scannedAt = Instant.fromEpochMilliseconds(0),
        skippedCount = 1,
        failedCount = 0,
      )
      tracker.stateFlow.value = completed

      assertThat(awaitItem().scanState).isEqualTo(completed)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_TabSelection_Updates() = runTest {
    presenter.test {
      val state = awaitItem()
      assertThat(state.selectedTab).isEqualTo(DiscoverTab.Upcoming)

      state.eventSink(DiscoverUiEvent.SelectTab(DiscoverTab.Missing))

      assertThat(awaitItem().selectedTab).isEqualTo(DiscoverTab.Missing)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_SeriesClick_NavigatesToSeriesDetail() = runTest {
    presenter.test {
      val state = awaitItem()

      state.eventSink(DiscoverUiEvent.SeriesClick("s1", "The Stormlight Archive"))

      assertThat(navigator.awaitNextScreen())
        .isEqualTo(SeriesDetailScreen("s1", "The Stormlight Archive"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_BookClick_OpensProviderUrl() = runTest {
    presenter.test {
      val state = awaitItem()

      state.eventSink(DiscoverUiEvent.BookClick("https://audible.com/pd/B00BWWSVPU"))

      assertThat(navigator.awaitNextScreen())
        .isEqualTo(UrlScreen("https://audible.com/pd/B00BWWSVPU"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_Back_PopsNavigator() = runTest {
    presenter.test {
      val state = awaitItem()

      state.eventSink(DiscoverUiEvent.Back)

      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }
  }
}
