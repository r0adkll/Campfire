// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.DevSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = DevSettings::class)
@Inject
class DevSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : DevSettings, AppSettings() {

  private val defaultDeveloperMode get() = false
  private val developerModeProperty = booleanSetting(KEY_DEVELOPER_MODE, defaultDeveloperMode)
  override var developerModeEnabled: Boolean by developerModeProperty

  override fun observeDeveloperMode(): StateFlow<Boolean> = developerModeProperty.observe()

  private val defaultSessionAge get() = 10.minutes
  private val sessionAgeProperty = durationSetting(KEY_SESSION_AGE, defaultSessionAge)
  override var sessionAge: Duration by sessionAgeProperty

  override fun observeSessionAge(): StateFlow<Duration> = sessionAgeProperty.observe()

  private val mediaButtonPackagesProperty = customSetting(
    key = KEY_MEDIA_BUTTON_PACKAGES,
    defaultValue = emptySet(),
    getter = { raw -> raw.decodePackageSet() },
    setter = { packages -> packages.encodePackageSet() },
  )
  private var mediaButtonPackages: Set<String> by mediaButtonPackagesProperty

  override fun observeMediaButtonPackages(): StateFlow<Set<String>> =
    mediaButtonPackagesProperty.observe()

  override fun recordMediaButtonPackage(packageName: String) {
    if (packageName.isBlank()) return
    val existing = mediaButtonPackages
    if (packageName in existing) return
    mediaButtonPackages = existing + packageName
  }

  override fun clearMediaButtonPackages() {
    mediaButtonPackages = emptySet()
  }

  private val fakeAppUpdateSignedInProperty = booleanSetting(KEY_FAKE_APP_UPDATE_SIGNED_IN, true)
  override var fakeAppUpdateSignedIn: Boolean by fakeAppUpdateSignedInProperty

  override fun observeFakeAppUpdateSignedIn(): StateFlow<Boolean> =
    fakeAppUpdateSignedInProperty.observe()

  private val fakeAppUpdateAvailableProperty = booleanSetting(KEY_FAKE_APP_UPDATE_AVAILABLE, false)
  override var fakeAppUpdateAvailable: Boolean by fakeAppUpdateAvailableProperty

  override fun observeFakeAppUpdateAvailable(): StateFlow<Boolean> =
    fakeAppUpdateAvailableProperty.observe()

  private val fakeAppUpdateFailDownloadProperty = booleanSetting(KEY_FAKE_APP_UPDATE_FAIL_DOWNLOAD, false)
  override var fakeAppUpdateFailDownload: Boolean by fakeAppUpdateFailDownloadProperty

  override fun observeFakeAppUpdateFailDownload(): StateFlow<Boolean> =
    fakeAppUpdateFailDownloadProperty.observe()
}

internal const val KEY_DEVELOPER_MODE = "pref_developer_mode_enabled"
internal const val KEY_SESSION_AGE = "pref_dev_setting_session_age"
internal const val KEY_MEDIA_BUTTON_PACKAGES = "pref_dev_setting_media_button_packages"
internal const val KEY_FAKE_APP_UPDATE_SIGNED_IN = "pref_dev_setting_fake_app_update_signed_in"
internal const val KEY_FAKE_APP_UPDATE_AVAILABLE = "pref_dev_setting_fake_app_update_available"
internal const val KEY_FAKE_APP_UPDATE_FAIL_DOWNLOAD = "pref_dev_setting_fake_app_update_fail_download"

private const val PACKAGE_SEPARATOR = "|"

private fun String.decodePackageSet(): Set<String> =
  if (isEmpty()) emptySet() else split(PACKAGE_SEPARATOR).filter { it.isNotEmpty() }.toSet()

private fun Set<String>.encodePackageSet(): String =
  joinToString(PACKAGE_SEPARATOR)
