// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.network

import app.campfire.core.model.Session
import app.campfire.network.models.PlaybackSession

interface NetworkSessionMapper {
  suspend fun map(session: Session): PlaybackSession
}
