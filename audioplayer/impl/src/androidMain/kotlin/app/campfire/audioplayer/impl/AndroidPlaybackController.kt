package app.campfire.audioplayer.impl

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.campfire.audioplayer.PlaybackController
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class AndroidPlaybackController(
  @ForScope(UserScope::class) private val scopeHolder: CoroutineScopeHolder,
) : PlaybackController {

  private var mediaController: MediaController? = null
    private set(value) {
      field = value
      mediaControllerFlow.value = value
    }

  private val mediaControllerFlow = MutableStateFlow<MediaController?>(null)

  @Composable
  override fun attachController() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
      bark(TAG) { "Requesting new MediaController connection" }
      // Create new token and build new controller
      val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
      val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
      controllerFuture.addListener(
        {
          mediaController = controllerFuture.get()
          bark(TAG) { "Acquired MediaController ($mediaController)" }
        },
        ContextCompat.getMainExecutor(context),
      )

      onDispose {
        bark(TAG) { "Disposing of media controller ($mediaController)" }
        MediaController.releaseFuture(controllerFuture)
        mediaController?.release()
        mediaController = null
      }
    }
  }

  override fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean,
  ) {
    mediaControllerFlow
      .filterNotNull()
      .take(1)
      .onEach { mediaController ->
        bark(TAG) { "$mediaController starting for $itemId, playImmediately=$playImmediately" }
        AudioPlayerService.start(mediaController, itemId, playImmediately)
      }
      .launchIn(scopeHolder.get())
  }

  override fun stopSession(itemId: LibraryItemId) {
    mediaControllerFlow
      .filterNotNull()
      .take(1)
      .onEach { mediaController ->
        bark(TAG) { "stopSession($mediaController)" }
        AudioPlayerService.stopSession(mediaController, itemId)
      }
      .launchIn(scopeHolder.get())
  }
}

private const val TAG = "AndroidPlaybackController"
