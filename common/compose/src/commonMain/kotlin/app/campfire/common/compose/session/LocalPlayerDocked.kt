// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.session

import androidx.compose.runtime.compositionLocalOf

/**
 * True while the player has a region of the screen to itself rather than floating over the
 * content — the lower half of a half-open foldable.
 *
 * Content insets key off this: with the floating bar suppressed there is nothing overlapping the
 * bottom of the content, so screens must not reserve a strip for it.
 */
val LocalPlayerDocked = compositionLocalOf { false }
