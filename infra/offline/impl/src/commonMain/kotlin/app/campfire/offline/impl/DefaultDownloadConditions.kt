// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline.impl

import app.campfire.core.di.AppScope
import app.campfire.network.reachability.WorkOffline
import app.campfire.offline.DownloadConditions
import app.campfire.settings.api.MobileDataSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Downloads may fetch unless working offline, or off Wi-Fi with "Download on Wi-Fi only" on. Where
 * the device can't tell Wi-Fi from mobile data, the Wi-Fi rule can't apply.
 */
@ContributesBinding(AppScope::class)
@Inject
class DefaultDownloadConditions(
  private val workOffline: WorkOffline,
  private val mobileDataSettings: MobileDataSettings,
) : DownloadConditions {

  override fun observeAllowed(): Flow<Boolean> = combine(
    workOffline.state,
    mobileDataSettings.observeDownloadOnWifiOnly(),
  ) { state, wifiOnly ->
    !state.isOffline && (!wifiOnly || !workOffline.canDetectWifi || state.onWifi)
  }.distinctUntilChanged()
}
