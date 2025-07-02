package app.campfire.widgets.callbacks

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import app.campfire.widgets.di.AudioPlayerActionCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForwardActionCallback : AudioPlayerActionCallback() {

  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    withContext(Dispatchers.Main) {
      audioPlayer?.seekForward()
    }
  }
}
