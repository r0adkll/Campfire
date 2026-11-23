// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.coroutines

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class FlowUtilsTest {

  @Test
  fun `flatMapLatestOrElse emits the fallback while the upstream is null`() = runTest {
    val upstream = MutableStateFlow<MutableStateFlow<Int>?>(null)

    upstream.flatMapLatestOrElse(-1) { it }.test {
      assertThat(awaitItem()).isEqualTo(-1)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `flatMapLatestOrElse stops collecting the previous value once the upstream goes null`() = runTest {
    val inner = MutableStateFlow(1)
    val upstream = MutableStateFlow<MutableStateFlow<Int>?>(inner)

    upstream.flatMapLatestOrElse(-1) { it }.test {
      assertThat(awaitItem()).isEqualTo(1)
      assertThat(inner.subscriptionCount.value).isEqualTo(1)

      upstream.value = null

      assertThat(awaitItem()).isEqualTo(-1)
      assertThat(inner.subscriptionCount.value).isEqualTo(0)
      inner.value = 2
      expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }
}
