package app.campfire.widgets.callbacks

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import app.campfire.widgets.di.AudioPlayerActionCallback

class PlayPauseActionCallback : AudioPlayerActionCallback() {

  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    if (audioPlayer == null) {
      // Cold start — use session-based approach that holds the MediaController
      // connection open during the full player.prepare() setup
      val session = getCurrentSession() ?: return
      component.oneShotPlaybackController.start(libraryItemId = session.libraryItem.id)
    } else {
      // Warm — service is running and playing, MediaController release is safe
      commandSender.playPause()
    }
  }
}
