// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.api.PlaybackHistoryUi
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayHostData
import com.slack.circuit.overlay.ReadOnlyOverlayApi
import kotlin.time.Duration

/** Records whose history was shown, and answers with [picks]. */
internal class FakePlaybackHistoryUi : PlaybackHistoryUi {
  val shown = mutableListOf<LibraryItemId>()
  var picks: Duration? = null

  /** Runs as the sheet closes, before what was picked is answered */
  var onClose: () -> Unit = {}

  override suspend fun showPlaybackHistory(overlayHost: OverlayHost, libraryItemId: LibraryItemId): Duration? {
    shown += libraryItemId
    onClose()
    return picks
  }
}

/** An overlay host that shows nothing, for a [FakePlaybackHistoryUi] that doesn't need one. */
@OptIn(ReadOnlyOverlayApi::class)
internal object NoOverlayHost : OverlayHost {
  override val currentOverlayData: OverlayHostData<Any>? = null

  override suspend fun <Result : Any> show(overlay: Overlay<Result>): Result = error("Nothing is shown")
}
