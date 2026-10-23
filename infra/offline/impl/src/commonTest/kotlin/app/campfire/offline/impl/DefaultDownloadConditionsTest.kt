// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline.impl

import app.campfire.network.reachability.OfflineChoice
import app.campfire.network.reachability.OfflineReason
import app.campfire.network.reachability.OfflineState
import app.campfire.network.reachability.WorkOffline
import app.campfire.offline.awaitAllowed
import app.campfire.offline.isAllowed
import app.campfire.settings.test.FakeMobileDataSettings
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class DefaultDownloadConditionsTest {

  private val workOffline = FakeWorkOffline()
  private val settings = FakeMobileDataSettings()
  private val conditions = DefaultDownloadConditions(workOffline, settings)

  @Test
  fun `downloads are allowed online`() = runTest {
    assertThat(conditions.isAllowed()).isTrue()
  }

  @Test
  fun `downloads wait while working offline`() = runTest {
    workOffline.state.value = OfflineState(reason = OfflineReason.UntilTurnedOff, onWifi = true)

    assertThat(conditions.isAllowed()).isFalse()
  }

  @Test
  fun `downloads kept to Wi-Fi wait off it`() = runTest {
    settings.downloadOnWifiOnly = true

    assertThat(conditions.isAllowed()).isFalse()

    workOffline.state.value = OfflineState(reason = null, onWifi = true)
    assertThat(conditions.isAllowed()).isTrue()
  }

  @Test
  fun `the Wi-Fi rule can't apply where Wi-Fi can't be told apart`() = runTest {
    workOffline.canDetectWifi = false
    settings.downloadOnWifiOnly = true

    assertThat(conditions.isAllowed()).isTrue()
  }

  @Test
  fun `waiting ends once downloads are allowed`() = runTest {
    settings.downloadOnWifiOnly = true
    var allowed = false
    launch {
      conditions.awaitAllowed()
      allowed = true
    }
    runCurrent()
    assertThat(allowed).isFalse()

    workOffline.state.value = OfflineState(reason = null, onWifi = true)
    runCurrent()

    assertThat(allowed).isTrue()
  }
}

private class FakeWorkOffline : WorkOffline {
  override val state = MutableStateFlow(OfflineState.Online)
  override var canDetectWifi: Boolean = true
  override fun isOffline(): Boolean = state.value.isOffline
  override fun goOffline(choice: OfflineChoice) = Unit
  override fun goOnline() = Unit
  override val sessionEnded: Flow<Unit> = emptyFlow()
}
