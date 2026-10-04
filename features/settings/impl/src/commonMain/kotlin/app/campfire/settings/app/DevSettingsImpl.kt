// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.app

import app.campfire.core.di.AppScope
import app.campfire.settings.api.DevSettings
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<DevSettings>())
@Inject
class DevSettingsImpl(
  override val store: SettingsStore,
) : DevSettings, AppSettings() {

  private val defaultDeveloperMode get() = false
  private val developerModeProperty = booleanSetting(KEY_DEVELOPER_MODE, defaultDeveloperMode)
  override fun setDeveloperModeEnabled(value: Boolean) = developerModeProperty.set(value)

  override fun observeDeveloperMode(): Flow<Boolean> = developerModeProperty.observe()

  private val defaultSessionAge get() = 10.minutes
  private val sessionAgeProperty = durationSetting(KEY_SESSION_AGE, defaultSessionAge)
  override fun setSessionAge(value: Duration) = sessionAgeProperty.set(value)

  override fun observeSessionAge(): Flow<Duration> = sessionAgeProperty.observe()

  private val hlsLargeItemThresholdProperty = durationSetting(KEY_HLS_LARGE_ITEM_THRESHOLD, 8.hours)
  override fun setHlsLargeItemThreshold(value: Duration) = hlsLargeItemThresholdProperty.set(value)

  override fun observeHlsLargeItemThreshold(): Flow<Duration> = hlsLargeItemThresholdProperty.observe()

  private val adaptToUnreachableServerProperty = booleanSetting(KEY_ADAPT_TO_UNREACHABLE_SERVER, true)
  override fun setAdaptToUnreachableServer(value: Boolean) = adaptToUnreachableServerProperty.set(value)

  override fun observeAdaptToUnreachableServer(): Flow<Boolean> = adaptToUnreachableServerProperty.observe()

  private val mediaButtonPackagesProperty = customSetting(
    key = KEY_MEDIA_BUTTON_PACKAGES,
    defaultValue = emptySet(),
    getter = { raw -> raw.decodePackageSet() },
    setter = { packages -> packages.encodePackageSet() },
  )
  override fun observeMediaButtonPackages(): Flow<Set<String>> =
    mediaButtonPackagesProperty.observe()

  override fun recordMediaButtonPackage(packageName: String) {
    if (packageName.isBlank()) return
    mediaButtonPackagesProperty.update { it + packageName }
  }

  override fun clearMediaButtonPackages() {
    mediaButtonPackagesProperty.set(emptySet())
  }

  private val fakeAppUpdateSignedInProperty = booleanSetting(KEY_FAKE_APP_UPDATE_SIGNED_IN, true)
  override fun setFakeAppUpdateSignedIn(value: Boolean) = fakeAppUpdateSignedInProperty.set(value)

  override fun observeFakeAppUpdateSignedIn(): Flow<Boolean> =
    fakeAppUpdateSignedInProperty.observe()

  private val fakeAppUpdateAvailableProperty = booleanSetting(KEY_FAKE_APP_UPDATE_AVAILABLE, false)
  override fun setFakeAppUpdateAvailable(value: Boolean) = fakeAppUpdateAvailableProperty.set(value)

  override fun observeFakeAppUpdateAvailable(): Flow<Boolean> =
    fakeAppUpdateAvailableProperty.observe()

  private val fakeAppUpdateFailDownloadProperty = booleanSetting(KEY_FAKE_APP_UPDATE_FAIL_DOWNLOAD, false)
  override fun setFakeAppUpdateFailDownload(value: Boolean) = fakeAppUpdateFailDownloadProperty.set(value)

  override fun observeFakeAppUpdateFailDownload(): Flow<Boolean> =
    fakeAppUpdateFailDownloadProperty.observe()
}

internal const val KEY_DEVELOPER_MODE = "pref_developer_mode_enabled"
internal const val KEY_SESSION_AGE = "pref_dev_setting_session_age"
internal const val KEY_HLS_LARGE_ITEM_THRESHOLD = "pref_dev_setting_hls_large_item_threshold"
internal const val KEY_ADAPT_TO_UNREACHABLE_SERVER = "pref_dev_setting_adapt_to_unreachable_server"
internal const val KEY_MEDIA_BUTTON_PACKAGES = "pref_dev_setting_media_button_packages"
internal const val KEY_FAKE_APP_UPDATE_SIGNED_IN = "pref_dev_setting_fake_app_update_signed_in"
internal const val KEY_FAKE_APP_UPDATE_AVAILABLE = "pref_dev_setting_fake_app_update_available"
internal const val KEY_FAKE_APP_UPDATE_FAIL_DOWNLOAD = "pref_dev_setting_fake_app_update_fail_download"

private const val PACKAGE_SEPARATOR = "|"

private fun String.decodePackageSet(): Set<String> =
  if (isEmpty()) emptySet() else split(PACKAGE_SEPARATOR).filter { it.isNotEmpty() }.toSet()

private fun Set<String>.encodePackageSet(): String =
  joinToString(PACKAGE_SEPARATOR)
