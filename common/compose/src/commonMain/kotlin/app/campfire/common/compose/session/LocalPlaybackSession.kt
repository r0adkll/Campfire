// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.session

import androidx.compose.runtime.compositionLocalOf
import app.campfire.core.model.Session

val LocalPlaybackSession = compositionLocalOf<Session?> { null }
