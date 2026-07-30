// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.api

import app.campfire.auth.api.model.ServerStatus
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.UserId

interface AuthRepository {

  suspend fun status(
    serverUrl: String,
    networkSettings: NetworkSettings? = null,
  ): Result<ServerStatus>

  suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    username: String,
    password: String,
    userId: UserId? = null,
    networkSettings: NetworkSettings? = null,
  ): Result<Unit>

  suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    codeVerifier: String,
    code: String,
    state: String,
    userId: UserId? = null,
    networkSettings: NetworkSettings? = null,
  ): Result<Unit>

  suspend fun getNetworkSettings(userId: UserId): NetworkSettings?
}
