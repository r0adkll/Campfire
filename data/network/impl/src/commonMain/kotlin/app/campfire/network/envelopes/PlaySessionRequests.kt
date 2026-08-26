// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import app.campfire.network.models.DeviceInfo
import kotlinx.serialization.Serializable

@Serializable
internal class PlayItemRequest(
  val deviceInfo: DeviceInfo,
  val mediaPlayer: String,
  val supportedMimeTypes: List<String>,
  val forceDirectPlay: Boolean,
  val forceTranscode: Boolean,
)

@Serializable
internal class SyncPlaybackSessionRequest(
  val currentTime: Double,
  val timeListened: Double,
  val duration: Double,
)

/** Serializes to `{}` — the deliberate empty body for closing a session without a final sync. */
@Serializable
internal class EmptyRequest
