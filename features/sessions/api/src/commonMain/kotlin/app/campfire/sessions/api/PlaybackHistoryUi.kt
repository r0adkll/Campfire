// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.api

import app.campfire.core.model.LibraryItemId
import com.slack.circuit.overlay.OverlayHost
import kotlin.time.Duration

/** Shows an item's playback history to surfaces outside the player. */
interface PlaybackHistoryUi {

  /**
   * Shows [libraryItemId]'s playback history. While the player has the item loaded an event can be
   * picked to jump to and the history cleared; otherwise it is only for reading.
   *
   * @return the position of the event picked, or null when none was
   */
  suspend fun showPlaybackHistory(
    overlayHost: OverlayHost,
    libraryItemId: LibraryItemId,
  ): Duration?
}
