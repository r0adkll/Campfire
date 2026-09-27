// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Duration

@ContributesBinding(AppScope::class)
@Inject
class DesktopWidgetUpdater : WidgetUpdater {
  override suspend fun updatePlayerWidget(
    currentTime: Duration?,
    currentDuration: Duration?,
    playbackSpeed: Float?,
  ) {
    // Do nothing
  }
}
