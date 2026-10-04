// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.AppStateSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [AppStateSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class TestAppStateSettings : AppStateSettings {

  private val _hasShownWidgetPinning = MutableStateFlow<Boolean>(false)
  val hasShownWidgetPinning: Boolean get() = _hasShownWidgetPinning.value
  override fun setHasShownWidgetPinning(value: Boolean) {
    _hasShownWidgetPinning.value = value
  }
  override fun observeHasShownWidgetPinning(): StateFlow<Boolean> = _hasShownWidgetPinning.asStateFlow()

  private val _lastSeenVersion = MutableStateFlow<String?>(null)
  val lastSeenVersion: String? get() = _lastSeenVersion.value
  override fun setLastSeenVersion(value: String?) {
    _lastSeenVersion.value = value
  }
  override fun observeLastSeenVersion(): StateFlow<String?> = _lastSeenVersion.asStateFlow()

  private val _appUpdateSignInDismissed = MutableStateFlow<Boolean>(false)
  val appUpdateSignInDismissed: Boolean get() = _appUpdateSignInDismissed.value
  override fun setAppUpdateSignInDismissed(value: Boolean) {
    _appUpdateSignInDismissed.value = value
  }
  override fun observeAppUpdateSignInDismissed(): StateFlow<Boolean> = _appUpdateSignInDismissed.asStateFlow()

  private val _appUpdateDismissedVersionCode = MutableStateFlow<Long>(0L)
  val appUpdateDismissedVersionCode: Long get() = _appUpdateDismissedVersionCode.value
  override fun setAppUpdateDismissedVersionCode(value: Long) {
    _appUpdateDismissedVersionCode.value = value
  }
  override fun observeAppUpdateDismissedVersionCode(): StateFlow<Long> = _appUpdateDismissedVersionCode.asStateFlow()
}
