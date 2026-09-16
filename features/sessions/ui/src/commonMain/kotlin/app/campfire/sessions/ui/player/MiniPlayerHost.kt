// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A host that can break the player out into its own always-on-top window — the desktop app.
 *
 * Provided through [LocalMiniPlayerHost] by the platform that owns the windows; the playback
 * bars only offer the break-out action when one is present. [isOpen] is snapshot state so the
 * action's icon and label follow the window.
 */
@Stable
interface MiniPlayerHost {
  val isOpen: Boolean
  fun open()
  fun close()
}

/** Null on every platform that cannot open a second window. */
val LocalMiniPlayerHost = staticCompositionLocalOf<MiniPlayerHost?> { null }
