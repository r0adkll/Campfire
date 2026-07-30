// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.api.di

import app.campfire.core.session.UserSession

interface UserGraphManager {

  fun create(userSession: UserSession)
  suspend fun destroy()
}
