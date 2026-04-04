package app.campfire.audioplayer.impl

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * This class spools up a media controller connection to start playback, then immediately tears it down
 * upon completion, or coroutine cancellation
 */
@Inject
class OneShotMediaControllerConnector(
  private val application: Application,
  private val dispatcherProvider: DispatcherProvider,
) {

  suspend fun fire(onConnected: suspend () -> Unit) {
    withController { onConnected() }
  }

  /**
   * Connect a temporary [MediaController] to the [AudioPlayerService], execute [block] with it,
   * then release the controller. This ensures the service is running and allows sending
   * transport or custom commands even when the app process is cold.
   */
  suspend fun <T> withController(block: suspend (MediaController) -> T): T = withContext(dispatcherProvider.main) {
    val future = suspendCancellableCoroutine { continuation ->
      // Create new token and build new controller
      val sessionToken = SessionToken(application, ComponentName(application, AudioPlayerService::class.java))
      val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
      controllerFuture.addListener(
        {
          continuation.resume(
            value = controllerFuture,
            onCancellation = { cause, value, context ->
              MediaController.releaseFuture(value)
            },
          )
        },
        ContextCompat.getMainExecutor(application),
      )

      continuation.invokeOnCancellation {
        MediaController.releaseFuture(controllerFuture)
      }
    }

    try {
      block(future.get())
    } finally {
      MediaController.releaseFuture(future)
    }
  }

  companion object : Cork {
    override val tag: String = "OneShotMediaControllerConnector"
  }
}
