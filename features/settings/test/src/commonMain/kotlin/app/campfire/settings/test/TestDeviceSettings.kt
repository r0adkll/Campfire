// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.core.model.UserId
import app.campfire.settings.api.DeviceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [DeviceSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class TestDeviceSettings : DeviceSettings {

  val deviceId: String = "test-deviceId"
  override suspend fun deviceId(): String = deviceId

  val analyticsId: String = "test-analyticsId"
  override suspend fun analyticsId(): String = analyticsId

  private val _currentUserId = MutableStateFlow<UserId?>(null)
  val currentUserId: UserId? get() = _currentUserId.value
  override fun setCurrentUserId(value: UserId?) {
    _currentUserId.value = value
  }
  override fun observeCurrentUserId(): StateFlow<UserId?> = _currentUserId.asStateFlow()
}
