// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.api

import app.campfire.core.session.UserSession
import kotlinx.coroutines.flow.StateFlow

interface UserSessionManager {

  var current: UserSession

  fun observe(): StateFlow<UserSession>
}
