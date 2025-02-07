package app.campfire.audioplayer.impl

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import me.tatarka.inject.annotations.Inject

/**
 * This class is responsible for connecting and disconnecting the [MediaController] instance
 * for any audio playback session. This is how you spool up, and down, the androidx media3 [AudioPlayerService]
 * for creating our [androidx.media3.exoplayer.ExoPlayer] instance and attaching all the media session
 * related integrations to get playback via notifications, watch, etc.
 */
@SingleIn(AppScope::class)
@Inject
class MediaControllerConnector(private val application: Application) {

  val mediaControllerFlow = MutableStateFlow<MediaController?>(null)

  private var controllerFuture: ListenableFuture<MediaController>? = null

  fun connect() {
    ibark { "~~> Requesting new MediaController connection" }

    // Create new token and build new controller
    val sessionToken = SessionToken(application, ComponentName(application, AudioPlayerService::class.java))
    controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
    controllerFuture!!.addListener(
      {
        mediaControllerFlow.value = controllerFuture!!.get().also {
          ibark { "<-- Acquired MediaController ($it)" }
        }
      },
      ContextCompat.getMainExecutor(application),
    )
  }

  fun disconnect() {
    ibark { "<!-- Disposing of media controller" }
    controllerFuture?.let { MediaController.releaseFuture(it) }
    mediaControllerFlow.value?.release()
    mediaControllerFlow.value = null
  }

  companion object : Cork {
    override val tag: String = "MediaControllerConnector"
  }
}
