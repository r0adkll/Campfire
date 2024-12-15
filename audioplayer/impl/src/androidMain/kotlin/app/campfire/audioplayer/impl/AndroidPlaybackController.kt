package app.campfire.audioplayer.impl

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.PlaybackController
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AndroidPlaybackController(
  private val application: Application,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : PlaybackController {

  override val currentPlayer = MutableStateFlow<AudioPlayer?>(null)

  private var mediaController: MediaController? = null

  override fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean,
  ) {
    if (mediaController?.isConnected != true) {
      // Create new token and build new controller
      val sessionToken = SessionToken(application, ComponentName(application, AudioPlayerService::class.java))
      val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
      controllerFuture.addListener(
        {
          // MediaController is available here with controllerFuture.get()
          mediaController = controllerFuture.get()

          // Start the session for this call
          AudioPlayerService.start(mediaController!!, itemId, playImmediately)

          // Listen for the activity lifecycle to die, then release any saved media controller
          applicationScope.launch {
            try {
              awaitCancellation()
            } finally {
              bark { "Releasing Media Controller!" }
              mediaController?.release()
            }
          }
        },
        ContextCompat.getMainExecutor(application),
      )
    } else {
      AudioPlayerService.start(mediaController!!, itemId, playImmediately)
    }
  }

  override fun stopSession(itemId: LibraryItemId) {
    mediaController?.let {
      AudioPlayerService.stopSession(it, itemId)
    }
    mediaController?.release()
    mediaController = null
  }
}
