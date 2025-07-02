package app.campfire.widgets.callbacks

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import app.campfire.widgets.di.AudioPlayerActionCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayPauseActionCallback : AudioPlayerActionCallback() {

  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    val session = getCurrentSession()
    if (audioPlayer == null && session != null) {
      // Player is not initialized, but session exists so let's bootstrap the service
      // and play the session.
      component.oneShotPlaybackController.start(
        libraryItemId = session.libraryItem.id,
      )
    } else {
      // Audio player already exists! So just command it
      withContext(Dispatchers.Main) {
        audioPlayer?.playPause()
      }
    }
  }
}
