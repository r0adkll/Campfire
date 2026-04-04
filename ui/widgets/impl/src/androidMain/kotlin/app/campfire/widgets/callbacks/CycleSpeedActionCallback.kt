package app.campfire.widgets.callbacks

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import app.campfire.widgets.di.AudioPlayerActionCallback

class CycleSpeedActionCallback : AudioPlayerActionCallback() {

  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    if (audioPlayer == null) return
    commandSender.cyclePlaybackSpeed()
  }
}
