package app.campfire.audioplayer.impl

import android.app.Activity
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.impl.model.Track
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class AndroidPlaybackController(
  private val activity: Activity,
  private val sessionRepository: SessionsRepository,
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : PlaybackController {

  override val currentPlayer = MutableStateFlow<AudioPlayer?>(null)

  private val activityScope: CoroutineScope get() =
    (activity as ComponentActivity).lifecycleScope

  private val sessionToken = SessionToken(activity, ComponentName(activity, AudioPlayerService::class.java))
  private var mediaController: MediaController? = null

  override fun startSession(itemId: LibraryItemId) {
    if (mediaController == null) {
      // Create new token and build new controller
      val controllerFuture = MediaController.Builder(activity, sessionToken).buildAsync()
      controllerFuture.addListener(
        {
          // MediaController is available here with controllerFuture.get()
          mediaController = controllerFuture.get()
          prepareSession(itemId)

          // Listen for the activity lifecycle to die, then release any saved media controller
          activityScope.launch {
            try {
              awaitCancellation()
            } finally {
              mediaController?.release()
            }
          }
        },
        ContextCompat.getMainExecutor(activity)
      )
    } else {
      prepareSession(itemId)
    }

    AudioPlayerService.start(activity)
  }

  override fun stopSession(itemId: LibraryItemId) {
    applicationScope.launch {
      sessionRepository.stopSession(itemId)
    }
    AudioPlayerService.stop(activity)
    mediaController?.release()
    mediaController = null
  }

  private fun prepareSession(itemId: LibraryItemId) {
    applicationScope.launch {
      val session = sessionRepository.createSession(itemId)
      bark { "Preparing playback session: $session" }

      val mediaItems = with(session.libraryItem) {
        media.tracks.map { track ->
          Track(
            id = "${id}_track${track.index}",
            track = track,
            media = media,
          ).asMediaItem
        }
      }
      withContext(dispatcherProvider.main) {
        mediaController?.run {
          addMediaItems(mediaItems)
          seekTo(session.currentTime.inWholeMilliseconds)
          playWhenReady = true
          prepare()
        }
      }
    }
  }
}
