// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

/**
 * Assigns [NetworkSnapshot.id] on platforms without a native per-connection handle: the id stays
 * while the same connection persists and advances when the device reconnects or its network
 * changes (different transports or subnet).
 */
internal fun NetworkSnapshot.withConnectionId(previous: NetworkSnapshot): NetworkSnapshot {
  val sameConnection = previous.connected && connected &&
    previous.transports == transports &&
    previous.fingerprint == fingerprint
  return copy(id = if (sameConnection) previous.id else previous.id + 1)
}
