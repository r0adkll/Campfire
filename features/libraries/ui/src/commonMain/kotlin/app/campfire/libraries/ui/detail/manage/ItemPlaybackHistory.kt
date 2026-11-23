// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.loadedPlayerFor
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.api.PlaybackHistoryUi
import com.slack.circuit.overlay.OverlayHost
import dev.zacsweers.metro.Inject

/**
 * [ItemAction.PlaybackHistory], from the item's detail menu and its card alike, so both offer the
 * same jumps: one can be picked only while the player has the item loaded.
 */
@Inject
class ItemPlaybackHistory(
  private val playbackHistoryUi: PlaybackHistoryUi,
  private val audioPlayerHolder: AudioPlayerHolder,
) {

  /** Shows [libraryItemId]'s history, and jumps to the moment picked from it. */
  suspend fun show(overlayHost: OverlayHost, libraryItemId: LibraryItemId) {
    Analytics.send(ActionEvent("playback_history", Click))
    val position = playbackHistoryUi.showPlaybackHistory(overlayHost, libraryItemId) ?: return
    // The player may have let the item go as the sheet closed
    audioPlayerHolder.loadedPlayerFor(libraryItemId)?.seekTo(position)
  }
}
