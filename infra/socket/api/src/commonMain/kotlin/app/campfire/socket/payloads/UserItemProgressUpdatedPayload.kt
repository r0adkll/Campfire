// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.payloads

import app.campfire.network.models.MediaProgress
import kotlinx.serialization.Serializable

@Serializable
data class UserItemProgressUpdatedPayload(
  val id: String,
  val data: MediaProgress,
)
