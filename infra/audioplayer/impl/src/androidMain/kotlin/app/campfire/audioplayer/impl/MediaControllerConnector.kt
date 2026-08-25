// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import android.app.Application
import android.content.ComponentName
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.Cork
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.flow.MutableStateFlow
import me.tatarka.inject.annotations.Inject

/**
 * This class is responsible for connecting and disconnecting the [MediaController] instance
 * for any audio playback session. This is how you spool up, and down, the androidx media3 [AudioPlayerService]
 * for creating our [androidx.media3.exoplayer.ExoPlayer] instance and attaching all the media session
 * related integrations to get playback via notifications, watch, etc.
 *
 * Connection follows [ProcessLifecycleOwner] rather than the host Activity: activity recreation
 * runs the old activity's `onStop` after the new activity's `onStart`, which would release the
 * controller the new activity just acquired.
 */
@SingleIn(AppScope::class)
@Inject
class MediaControllerConnector(private val application: Application) {

  val mediaControllerFlow = MutableStateFlow<MediaController?>(null)

  private var controllerFuture: ListenableFuture<MediaController>? = null

  private var initialized = false

  private val processLifecycleObserver = object : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) = connect()
    override fun onStop(owner: LifecycleOwner) = disconnect()
  }

  /**
   * Called once at app startup by [MediaControllerConnectorInitializer]. Registers the
   * process-lifecycle observer that connects/disconnects with app foreground state.
   */
  @MainThread
  fun initialize() {
    if (initialized) return
    initialized = true
    ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
  }

  fun connect() {
    if (controllerFuture != null) return
    ibark { "~~> Requesting new MediaController connection" }

    // Create new token and build new controller
    val sessionToken = SessionToken(application, ComponentName(application, AudioPlayerService::class.java))
    val future = MediaController.Builder(application, sessionToken).buildAsync()
    controllerFuture = future
    future.addListener(
      {
        try {
          mediaControllerFlow.value = future.get().also {
            ibark { "<-- Acquired MediaController ($it)" }
          }
        } catch (e: CancellationException) {
          ebark(throwable = e) { "MediaController connection was cancelled" }
        } catch (e: ExecutionException) {
          ebark(throwable = e) { "MediaController connection threw an exception" }
          if (controllerFuture === future) controllerFuture = null
        } catch (e: InterruptedException) {
          ebark(throwable = e) { "MediaController connection was interrupted" }
          if (controllerFuture === future) controllerFuture = null
        }
      },
      ContextCompat.getMainExecutor(application),
    )
  }

  fun disconnect() {
    ibark { "<!-- Disposing of media controller" }
    controllerFuture?.let { MediaController.releaseFuture(it) }
    controllerFuture = null
    mediaControllerFlow.value?.release()
    mediaControllerFlow.value = null
  }

  companion object : Cork {
    override val tag: String = "MediaControllerConnector"
  }
}
