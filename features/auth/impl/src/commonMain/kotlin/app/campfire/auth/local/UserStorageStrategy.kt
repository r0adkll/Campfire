// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.local

import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User as NetworkUser

interface UserStorageStrategy {

  suspend fun store(
    serverName: String,
    serverUrl: String,
    serverSettings: ServerSettings,
    user: NetworkUser,
    userDefaultLibraryId: String,
  )
}
