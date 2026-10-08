// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.core.model.UserId
import app.campfire.settings.api.DeviceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestDeviceSettings(
  private val testScope: CoroutineScope = TestScope(UnconfinedTestDispatcher()),
) : TestSettings(), DeviceSettings {

  override var deviceId: String by string()

  override var analyticsId: String by string()

  override var currentUserId: UserId? by stringOrNull()
  override fun observeCurrentUserId(): StateFlow<UserId?> =
    observeStringOrNull(::currentUserId)
      .stateIn(testScope, SharingStarted.Lazily, currentUserId)
}
