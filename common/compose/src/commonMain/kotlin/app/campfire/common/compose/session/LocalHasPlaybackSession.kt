// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.session

import androidx.compose.runtime.compositionLocalOf

/**
 * True while the user has a playback session, i.e. whenever the playback bar has something to show.
 *
 * Deliberately a flag rather than the session itself: the session's position is rewritten on every
 * player tick, and every screen root reads this through [app.campfire.common.compose.CampfireWindowInsets],
 * so providing the session would recompose every visible screen twice a second during playback.
 */
val LocalHasPlaybackSession = compositionLocalOf { false }
