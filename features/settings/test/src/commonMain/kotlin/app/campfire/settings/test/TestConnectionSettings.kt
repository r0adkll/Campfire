// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.ConnectionSettings
import app.campfire.settings.api.OfflineSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestConnectionSettings(
  private val testScope: CoroutineScope = TestScope(UnconfinedTestDispatcher()),
) : TestSettings(), ConnectionSettings {

  override var socketEnabled: Boolean by boolean()
  override fun observeSocketEnabled(): StateFlow<Boolean> =
    observeBoolean(::socketEnabled)
      .stateIn(testScope, SharingStarted.Lazily, socketEnabled)

  private val offlineSessionFlow = MutableStateFlow<OfflineSession>(OfflineSession.None)
  override var offlineSession: OfflineSession
    get() = offlineSessionFlow.value
    set(value) {
      offlineSessionFlow.value = value
    }
  override fun observeOfflineSession(): StateFlow<OfflineSession> = offlineSessionFlow.asStateFlow()

  private val workOfflineOnMobileDataFlow = MutableStateFlow(false)
  override var workOfflineOnMobileData: Boolean
    get() = workOfflineOnMobileDataFlow.value
    set(value) {
      workOfflineOnMobileDataFlow.value = value
    }
  override fun observeWorkOfflineOnMobileData(): StateFlow<Boolean> = workOfflineOnMobileDataFlow.asStateFlow()

  private val mobileDataOfflineSuspendedFlow = MutableStateFlow(false)
  override var mobileDataOfflineSuspended: Boolean
    get() = mobileDataOfflineSuspendedFlow.value
    set(value) {
      mobileDataOfflineSuspendedFlow.value = value
    }
  override fun observeMobileDataOfflineSuspended(): StateFlow<Boolean> = mobileDataOfflineSuspendedFlow.asStateFlow()
}
