// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.app

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest

class MobileDataSettingsTest {

  private val scope = CoroutineScope(Dispatchers.Unconfined + Job())

  @AfterTest
  fun tearDown() {
    scope.cancel()
  }

  private fun settings(vararg initial: Pair<String, Any>) =
    MobileDataSettingsImpl(MapSettings(initial.toMap().toMutableMap()), scope)

  @Test
  fun `defaults keep a home server off mobile data and allow downloads on it`() {
    assertThat(settings().homeServerOnMobileData).isFalse()
    assertThat(settings().downloadOnWifiOnly).isFalse()
  }

  @Test
  fun `the earlier switch carries over`() {
    assertThat(settings(KEY_LEGACY_SKIP_HOME_SERVER_ON_MOBILE_DATA to false).homeServerOnMobileData).isTrue()
    assertThat(settings(KEY_LEGACY_SKIP_HOME_SERVER_ON_MOBILE_DATA to true).homeServerOnMobileData).isFalse()
  }

  @Test
  fun `changes are observed`() = runTest {
    val settings = settings()

    settings.observeHomeServerOnMobileData().test {
      assertThat(awaitItem()).isFalse()

      settings.setHomeServerOnMobileData(true)
      assertThat(awaitItem()).isTrue()
    }
  }
}
