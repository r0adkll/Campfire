// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.navigation

import androidx.compose.runtime.compositionLocalOf
import app.campfire.core.session.UserSession

val LocalUserSession = compositionLocalOf<UserSession> {
  error("No user session provided in this composition")
}
