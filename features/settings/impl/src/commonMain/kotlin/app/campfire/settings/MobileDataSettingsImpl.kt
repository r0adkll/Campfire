// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.MobileDataSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<MobileDataSettings>())
@Inject
class MobileDataSettingsImpl(
  @SettingsStore override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : MobileDataSettings, AppSettings() {

  // Until a choice is stored, carry over the earlier "skip my home server on mobile data" switch
  // (on by default), which stored the inverse of this setting
  private val homeServerOnMobileDataProperty = booleanSetting(
    key = KEY_HOME_SERVER_ON_MOBILE_DATA,
    defaultValue = !settings.getBoolean(KEY_LEGACY_SKIP_HOME_SERVER_ON_MOBILE_DATA, true),
  )
  override var homeServerOnMobileData: Boolean by homeServerOnMobileDataProperty
  override fun observeHomeServerOnMobileData(): StateFlow<Boolean> = homeServerOnMobileDataProperty.observe()

  private val downloadOnWifiOnlyProperty = booleanSetting(KEY_DOWNLOAD_ON_WIFI_ONLY, false)
  override var downloadOnWifiOnly: Boolean by downloadOnWifiOnlyProperty
  override fun observeDownloadOnWifiOnly(): StateFlow<Boolean> = downloadOnWifiOnlyProperty.observe()
}

internal const val KEY_HOME_SERVER_ON_MOBILE_DATA = "pref_home_server_on_mobile_data"
internal const val KEY_LEGACY_SKIP_HOME_SERVER_ON_MOBILE_DATA = "pref_pause_away_from_home"
internal const val KEY_DOWNLOAD_ON_WIFI_ONLY = "pref_download_on_wifi_only"
