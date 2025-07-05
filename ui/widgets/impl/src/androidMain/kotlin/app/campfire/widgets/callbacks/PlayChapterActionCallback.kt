package app.campfire.widgets.callbacks

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import app.campfire.widgets.di.AudioPlayerActionCallback
import kotlin.IllegalStateException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayChapterActionCallback() : AudioPlayerActionCallback() {

  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    val itemId = parameters[KEY_ITEM_ID] ?: return
    val chapterId = parameters[KEY_CHAPTER_ID] ?: return

    val currentSession = getCurrentSession() ?: return
    if (itemId == currentSession.libraryItem.id && audioPlayer != null) {
      // Just seek to the chapter id
      withContext(Dispatchers.Main) {
        audioPlayer?.seekTo(chapterId)
          ?: throw IllegalStateException("Current session doesn't have a player")
      }
    } else {
      // Start a new session for the item at the given chapter
      component.oneShotPlaybackController.start(itemId, true, chapterId)
    }
  }

  companion object {
    val KEY_ITEM_ID = ActionParameters.Key<String>("item_id")
    val KEY_CHAPTER_ID = ActionParameters.Key<Int>("chapter_id")
  }
}
