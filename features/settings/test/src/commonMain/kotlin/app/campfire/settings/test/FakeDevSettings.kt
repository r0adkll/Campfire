// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.DevSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A simple in-memory [DevSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakeDevSettings : DevSettings {

  private val _developerModeEnabled = MutableStateFlow(false)
  override var developerModeEnabled: Boolean
    get() = _developerModeEnabled.value
    set(value) { _developerModeEnabled.value = value }
  override fun observeDeveloperMode(): StateFlow<Boolean> = _developerModeEnabled.asStateFlow()

  private val _sessionAge = MutableStateFlow(10.minutes)
  override var sessionAge: Duration
    get() = _sessionAge.value
    set(value) { _sessionAge.value = value }
  override fun observeSessionAge(): StateFlow<Duration> = _sessionAge.asStateFlow()

  private val _hlsLargeItemThreshold = MutableStateFlow(8.hours)
  override var hlsLargeItemThreshold: Duration
    get() = _hlsLargeItemThreshold.value
    set(value) { _hlsLargeItemThreshold.value = value }
  override fun observeHlsLargeItemThreshold(): StateFlow<Duration> = _hlsLargeItemThreshold.asStateFlow()

  private val _adaptToUnreachableServer = MutableStateFlow(true)
  override var adaptToUnreachableServer: Boolean
    get() = _adaptToUnreachableServer.value
    set(value) { _adaptToUnreachableServer.value = value }
  override fun observeAdaptToUnreachableServer(): StateFlow<Boolean> = _adaptToUnreachableServer.asStateFlow()

  private val _mediaButtonPackages = MutableStateFlow<Set<String>>(emptySet())
  override fun observeMediaButtonPackages(): StateFlow<Set<String>> = _mediaButtonPackages.asStateFlow()
  override fun recordMediaButtonPackage(packageName: String) {
    _mediaButtonPackages.value += packageName
  }
  override fun clearMediaButtonPackages() {
    _mediaButtonPackages.value = emptySet()
  }

  private val _fakeAppUpdateSignedIn = MutableStateFlow(true)
  override var fakeAppUpdateSignedIn: Boolean
    get() = _fakeAppUpdateSignedIn.value
    set(value) { _fakeAppUpdateSignedIn.value = value }
  override fun observeFakeAppUpdateSignedIn(): StateFlow<Boolean> = _fakeAppUpdateSignedIn.asStateFlow()

  private val _fakeAppUpdateAvailable = MutableStateFlow(false)
  override var fakeAppUpdateAvailable: Boolean
    get() = _fakeAppUpdateAvailable.value
    set(value) { _fakeAppUpdateAvailable.value = value }
  override fun observeFakeAppUpdateAvailable(): StateFlow<Boolean> = _fakeAppUpdateAvailable.asStateFlow()

  private val _fakeAppUpdateFailDownload = MutableStateFlow(false)
  override var fakeAppUpdateFailDownload: Boolean
    get() = _fakeAppUpdateFailDownload.value
    set(value) { _fakeAppUpdateFailDownload.value = value }
  override fun observeFakeAppUpdateFailDownload(): StateFlow<Boolean> = _fakeAppUpdateFailDownload.asStateFlow()
}
