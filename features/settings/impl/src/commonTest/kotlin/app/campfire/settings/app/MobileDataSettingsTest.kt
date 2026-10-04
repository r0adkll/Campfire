// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.app

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.campfire.settings.store.InMemoryPreferencesDataStore
import app.campfire.settings.store.testSettingsStore
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class MobileDataSettingsTest {

  /** Settings migrated from before DataStore, where [legacySkipHomeServer] was the earlier switch. */
  private fun settings(legacySkipHomeServer: Boolean? = null): MobileDataSettingsImpl {
    val stored = mutablePreferencesOf()
    legacySkipHomeServer?.let { stored[booleanPreferencesKey(KEY_LEGACY_SKIP_HOME_SERVER_ON_MOBILE_DATA)] = it }
    return MobileDataSettingsImpl(testSettingsStore(InMemoryPreferencesDataStore(stored)))
  }

  @Test
  fun `defaults keep a home server off mobile data and allow downloads on it`() = runTest {
    assertThat(settings().observeHomeServerOnMobileData().first()).isFalse()
    assertThat(settings().observeDownloadOnWifiOnly().first()).isFalse()
  }

  @Test
  fun `the earlier switch carries over`() = runTest {
    val skippedHomeServer = settings(legacySkipHomeServer = true)
    val allowedHomeServer = settings(legacySkipHomeServer = false)
    assertThat(allowedHomeServer.observeHomeServerOnMobileData().first()).isTrue()
    assertThat(skippedHomeServer.observeHomeServerOnMobileData().first()).isFalse()
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
