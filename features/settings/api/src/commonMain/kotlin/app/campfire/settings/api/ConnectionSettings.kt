// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

/**
 * How Campfire stays connected to the server.
 */
interface ConnectionSettings {

  /**
   * When `true`, the realtime Socket.IO connection to the ABS server is enabled and the client
   * receives live updates (library/series/collection/podcast changes, media progress, etc.).
   * When `false`, the socket stays disconnected and [app.campfire.socket.SocketState.Disabled]
   * is exposed so UI indicators can hide.
   *
   * Defaults to `true`.
   */
  var socketEnabled: Boolean
  fun observeSocketEnabled(): StateFlow<Boolean>

  /**
   * The stretch of working offline the user started, if any. While working offline Campfire makes
   * no requests to the server — API, cover art, streaming, downloads or the socket — and runs from
   * downloaded and cached content; progress recorded meanwhile syncs once it ends. Persists across
   * launches. Start and end it through `WorkOffline`, which also ends it when its time runs out or
   * Wi-Fi returns.
   *
   * Defaults to [OfflineSession.None].
   */
  var offlineSession: OfflineSession
  fun observeOfflineSession(): StateFlow<OfflineSession>

  /**
   * When `true`, Campfire also works offline whenever the device is on mobile data alone.
   *
   * Defaults to `false`.
   */
  var workOfflineOnMobileData: Boolean
  fun observeWorkOfflineOnMobileData(): StateFlow<Boolean>

  /**
   * Set when the user goes online despite [workOfflineOnMobileData]: the rule then stands aside
   * until the device is next on Wi-Fi or Ethernet, which clears this.
   *
   * Defaults to `false`.
   */
  var mobileDataOfflineSuspended: Boolean
  fun observeMobileDataOfflineSuspended(): StateFlow<Boolean>
}
