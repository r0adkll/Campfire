package app.campfire.sessions.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.PlaybackController
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.Session
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.launch

@ContributesTo(UserScope::class)
interface SessionHostComponent {
  val sessionsRepository: SessionsRepository
  val playbackController: PlaybackController
  val audioPlayerHolder: AudioPlayerHolder
}

@Composable
private fun rememberSessionHostComponent(): SessionHostComponent {
  return remember { ComponentHolder.component() }
}

@Composable
fun SessionHostLayout(
  component: SessionHostComponent = rememberSessionHostComponent(),
  content: @Composable (session: Session?, player: AudioPlayer?, clearSession: () -> Unit) -> Unit,
) {
  val scope = rememberCoroutineScope()

  val currentSession by remember {
    component.sessionsRepository.observeCurrentSession()
  }.collectAsState(null)

  val audioPlayer by remember {
    component.audioPlayerHolder.currentPlayer
  }.collectAsState()

  // Attach the playback controller to the lifecycle of this composition.
  component.playbackController.attachController()

  LaunchedEffect(currentSession) {
    if (currentSession != null && audioPlayer == null) {
      // If the current session exists but the audio player is not initialized yet, initialize it
      bark(LogPriority.WARN) { "Session found, but media player not initialized, starting…" }
      component.playbackController.startSession(
        itemId = currentSession!!.libraryItem.id,
        playImmediately = false,
      )
    }
  }

  val clearSession: () -> Unit = remember {
    {
      currentSession?.let { s ->
        scope.launch {
          component.playbackController.stopSession(s.libraryItem.id)
        }
      }
    }
  }

  content(currentSession, audioPlayer, clearSession)
}
