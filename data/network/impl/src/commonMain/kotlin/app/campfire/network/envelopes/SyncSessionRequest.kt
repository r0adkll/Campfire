// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import app.campfire.network.models.DeviceInfo
import app.campfire.network.models.PlaybackSession
import kotlinx.serialization.Serializable

@Serializable
class SyncSessionRequest(
  val deviceInfo: DeviceInfo,
  val sessions: List<PlaybackSession>,
)
