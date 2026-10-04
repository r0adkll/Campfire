// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.Flow

interface MobileDataSettings {

  /**
   * Whether Campfire connects to a server with a local address (e.g. `192.168.x.x`) while the
   * device is on mobile data alone. Such an address normally can't be reached that way without a
   * VPN (which is always allowed), so by default Campfire waits for Wi-Fi.
   * Default: `false`
   */
  fun setHomeServerOnMobileData(value: Boolean)
  fun observeHomeServerOnMobileData(): Flow<Boolean>

  /**
   * Whether downloads wait for Wi-Fi (an unmetered connection) instead of using mobile data.
   * Default: `false`
   */
  fun setDownloadOnWifiOnly(value: Boolean)
  fun observeDownloadOnWifiOnly(): Flow<Boolean>
}
