// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets

import kotlin.time.Duration

interface WidgetUpdater {

  /**
   * Update any homescreen widgets with the lastest information from the app.
   */
  suspend fun updatePlayerWidget(
    currentTime: Duration? = null,
    currentDuration: Duration? = null,
    playbackSpeed: Float? = null,
  )
}
