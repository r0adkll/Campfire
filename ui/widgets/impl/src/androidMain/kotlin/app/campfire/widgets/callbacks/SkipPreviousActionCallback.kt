// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.callbacks

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import app.campfire.widgets.di.AudioPlayerActionCallback

class SkipPreviousActionCallback : AudioPlayerActionCallback() {

  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    if (audioPlayer == null) return
    commandSender.skipToPrevious()
  }
}
