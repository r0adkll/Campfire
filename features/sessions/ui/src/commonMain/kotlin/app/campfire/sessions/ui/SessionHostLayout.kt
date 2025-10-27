package app.campfire.sessions.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
private fun rememberSessionHostComponent(): State<SessionHostComponent> {
  return remember {
    ComponentHolder.subscribe<SessionHostComponent>()
  }.collectAsState(ComponentHolder.component<SessionHostComponent>())
}

@Composable
fun SessionHostLayout(
  component: State<SessionHostComponent> = rememberSessionHostComponent(),
  content: @Composable (session: Session?, player: AudioPlayer?, clearSession: () -> Unit) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val comp by component

  val currentSession by remember {
    comp.sessionsRepository.observeCurrentSession()
  }.collectAsState(null)

  val audioPlayer by remember {
    comp.audioPlayerHolder.currentPlayer
  }.collectAsState()

  val currentSessionId = currentSession?.id
  val audioPlayerMiniState = audioPlayer?.preparedSession?.id?.let { preparedSessionId ->
    preparedSessionId to audioPlayer!!.state.value
  }

  LaunchedEffect(currentSessionId, audioPlayerMiniState) {
    if (
      currentSessionId != null &&
      (
        (audioPlayerMiniState == null) ||
          currentSessionId != audioPlayerMiniState.first ||
          audioPlayerMiniState.second == AudioPlayer.State.Disabled
        )
    ) {
      // If the current session exists but the audio player is not initialized yet, initialize it
      bark(LogPriority.WARN) {
        """
          Session(
            currentSession = ${currentSession?.id},
            audioPlayer = $audioPlayer,
            audioPlayer.state = ${audioPlayer?.state?.value},
            audioPlayer.session = ${audioPlayer?.preparedSession?.id},
          ) found, but media player not initialized, starting
        """.trimIndent()
      }
      comp.playbackController.startSession(
        itemId = currentSession!!.libraryItem.id,
        playImmediately = false,
      )
    }
  }

  val clearSession: () -> Unit = remember {
    {
      currentSession?.let { s ->
        scope.launch {
          comp.playbackController.stopSession(s.libraryItem.id)
        }
      }
    }
  }

  content(currentSession, audioPlayer, clearSession)
}
