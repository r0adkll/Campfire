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
}
