// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
  val username: String,
  val password: String,
)

@Serializable
data class LoginResponse(
  val user: User,
  // Null when the user has no accessible libraries on the server
  val userDefaultLibraryId: String? = null,
  val serverSettings: ServerSettings,
  @SerialName("Source") val source: String,
)
