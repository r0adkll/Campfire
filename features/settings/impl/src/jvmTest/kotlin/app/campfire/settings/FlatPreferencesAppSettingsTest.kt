// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import com.russhwolf.settings.ObservableSettings
import kotlin.test.AfterTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class FlatPreferencesAppSettingsTest : AppSettingsTest() {

  // As in the app: listeners run on a background dispatcher, not the caller's
  private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun createObservableSettings(): ObservableSettings =
    FlatPreferencesSettings(openSettingsStore(newSettingsFile(), migrations = emptyList()), listenerScope)

  @AfterTest
  fun cancelListeners() {
    listenerScope.cancel()
  }
}
