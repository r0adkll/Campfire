// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

interface LocalServerSettings {

  /**
   * Whether Campfire skips reaching a server with a local address (e.g. `192.168.x.x`) while the
   * device is on mobile data alone, where such an address can't be reached without a VPN.
   * Default: `true`
   */
  var avoidMobileData: Boolean
  fun observeAvoidMobileData(): StateFlow<Boolean>
}
