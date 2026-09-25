// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.HomeNetworkSettings
import app.campfire.settings.api.LearnedHomeNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A simple in-memory [HomeNetworkSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakeHomeNetworkSettings : HomeNetworkSettings {

  private val _pauseAwayFromHome = MutableStateFlow(true)
  override var pauseAwayFromHome: Boolean
    get() = _pauseAwayFromHome.value
    set(value) { _pauseAwayFromHome.value = value }
  override fun observePauseAwayFromHome(): StateFlow<Boolean> = _pauseAwayFromHome.asStateFlow()

  private val _learnedHomeNetworks = MutableStateFlow<List<LearnedHomeNetwork>>(emptyList())
  override var learnedHomeNetworks: List<LearnedHomeNetwork>
    get() = _learnedHomeNetworks.value
    set(value) { _learnedHomeNetworks.value = value }
  override fun observeLearnedHomeNetworks(): StateFlow<List<LearnedHomeNetwork>> = _learnedHomeNetworks.asStateFlow()
}
