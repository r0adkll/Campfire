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
 * An in-memory [DevSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakeDevSettings : DevSettings {

  private val _developerModeEnabled = MutableStateFlow<Boolean>(false)
  override val developerModeEnabled: Boolean get() = _developerModeEnabled.value
  override fun setDeveloperModeEnabled(value: Boolean) {
    _developerModeEnabled.value = value
  }
  override fun observeDeveloperMode(): StateFlow<Boolean> = _developerModeEnabled.asStateFlow()

  private val _sessionAge = MutableStateFlow<Duration>(10.minutes)
  override val sessionAge: Duration get() = _sessionAge.value
  override fun setSessionAge(value: Duration) {
    _sessionAge.value = value
  }
  override fun observeSessionAge(): StateFlow<Duration> = _sessionAge.asStateFlow()

  private val _hlsLargeItemThreshold = MutableStateFlow<Duration>(8.hours)
  override val hlsLargeItemThreshold: Duration get() = _hlsLargeItemThreshold.value
  override fun setHlsLargeItemThreshold(value: Duration) {
    _hlsLargeItemThreshold.value = value
  }
  override fun observeHlsLargeItemThreshold(): StateFlow<Duration> = _hlsLargeItemThreshold.asStateFlow()

  private val _adaptToUnreachableServer = MutableStateFlow<Boolean>(true)
  override val adaptToUnreachableServer: Boolean get() = _adaptToUnreachableServer.value
  override fun setAdaptToUnreachableServer(value: Boolean) {
    _adaptToUnreachableServer.value = value
  }
  override fun observeAdaptToUnreachableServer(): StateFlow<Boolean> = _adaptToUnreachableServer.asStateFlow()

  private val _fakeAppUpdateSignedIn = MutableStateFlow<Boolean>(true)
  override val fakeAppUpdateSignedIn: Boolean get() = _fakeAppUpdateSignedIn.value
  override fun setFakeAppUpdateSignedIn(value: Boolean) {
    _fakeAppUpdateSignedIn.value = value
  }
  override fun observeFakeAppUpdateSignedIn(): StateFlow<Boolean> = _fakeAppUpdateSignedIn.asStateFlow()

  private val _fakeAppUpdateAvailable = MutableStateFlow<Boolean>(false)
  override val fakeAppUpdateAvailable: Boolean get() = _fakeAppUpdateAvailable.value
  override fun setFakeAppUpdateAvailable(value: Boolean) {
    _fakeAppUpdateAvailable.value = value
  }
  override fun observeFakeAppUpdateAvailable(): StateFlow<Boolean> = _fakeAppUpdateAvailable.asStateFlow()

  private val _fakeAppUpdateFailDownload = MutableStateFlow<Boolean>(false)
  override val fakeAppUpdateFailDownload: Boolean get() = _fakeAppUpdateFailDownload.value
  override fun setFakeAppUpdateFailDownload(value: Boolean) {
    _fakeAppUpdateFailDownload.value = value
  }
  override fun observeFakeAppUpdateFailDownload(): StateFlow<Boolean> = _fakeAppUpdateFailDownload.asStateFlow()

  private val _mediaButtonPackages = MutableStateFlow<Set<String>>(emptySet())
  override fun observeMediaButtonPackages(): StateFlow<Set<String>> = _mediaButtonPackages.asStateFlow()

  override fun recordMediaButtonPackage(packageName: String) {
    _mediaButtonPackages.value += packageName
  }

  override fun clearMediaButtonPackages() {
    _mediaButtonPackages.value = emptySet()
  }
}
