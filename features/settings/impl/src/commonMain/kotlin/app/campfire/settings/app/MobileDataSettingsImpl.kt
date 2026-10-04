// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.app

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.MobileDataSettings
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsDispatcher
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<MobileDataSettings>())
@Inject
class MobileDataSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
  @SettingsDispatcher override val dispatcher: CoroutineDispatcher,
) : MobileDataSettings, AppSettings() {

  // Until a choice is stored, carry over the earlier "skip my home server on mobile data" switch
  // (on by default), which stored the inverse of this setting
  private val homeServerOnMobileDataProperty = booleanSetting(
    key = KEY_HOME_SERVER_ON_MOBILE_DATA,
    defaultValue = !settings.getBoolean(KEY_LEGACY_SKIP_HOME_SERVER_ON_MOBILE_DATA, true),
  )
  override fun setHomeServerOnMobileData(value: Boolean) = homeServerOnMobileDataProperty.set(value)
  override fun observeHomeServerOnMobileData(): Flow<Boolean> = homeServerOnMobileDataProperty.observe()

  private val downloadOnWifiOnlyProperty = booleanSetting(KEY_DOWNLOAD_ON_WIFI_ONLY, false)
  override fun setDownloadOnWifiOnly(value: Boolean) = downloadOnWifiOnlyProperty.set(value)
  override fun observeDownloadOnWifiOnly(): Flow<Boolean> = downloadOnWifiOnlyProperty.observe()
}

internal const val KEY_HOME_SERVER_ON_MOBILE_DATA = "pref_home_server_on_mobile_data"
internal const val KEY_LEGACY_SKIP_HOME_SERVER_ON_MOBILE_DATA = "pref_pause_away_from_home"
internal const val KEY_DOWNLOAD_ON_WIFI_ONLY = "pref_download_on_wifi_only"
