// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.collections.ui.list

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.common.screens.CollectionsScreen
import app.campfire.settings.test.TestCampfireSettings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest

class CollectionsPresenterTest {

  @Test
  fun eventSink_Refresh_refreshesCollectionsOnceWhileShowingProgress() = runTest {
    val refreshGate = CompletableDeferred<Unit>()
    val repository = FakeCollectionsRepository(onRefreshCollections = { refreshGate.await() })
    val presenter = CollectionsPresenter(
      navigator = FakeNavigator(CollectionsScreen),
      repository = repository,
      settings = TestCampfireSettings(),
      analytics = FakeAnalytics(),
    )

    presenter.test {
      val idle = awaitItem()
      assertThat(idle.isRefreshing).isFalse()

      idle.eventSink(CollectionsUiEvent.Refresh)
      val refreshing = awaitItem()
      assertThat(refreshing.isRefreshing).isTrue()

      // Pulling again while the first refresh is in flight doesn't start a second one
      refreshing.eventSink(CollectionsUiEvent.Refresh)
      refreshGate.complete(Unit)

      assertThat(awaitItem().isRefreshing).isFalse()
      assertThat(repository.refreshCount).isEqualTo(1)
    }
  }
}
