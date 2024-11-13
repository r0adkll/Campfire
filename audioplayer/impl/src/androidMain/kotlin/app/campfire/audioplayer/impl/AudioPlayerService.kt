package app.campfire.audioplayer.impl

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@ContributesTo(UserScope::class)
interface AudioPlayerComponent {
  val audioPlaybackController: AndroidPlaybackController
  val exoPlayerFactory: ExoPlayerAudioPlayer.Factory
  val sessionsRepository: SessionsRepository
}

@SuppressLint("UnsafeOptInUsageError")
class AudioPlayerService : MediaSessionService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private lateinit var player: ExoPlayerAudioPlayer
  private var session: MediaSession? = null

  private val component by lazy {
    ComponentHolder.component<AudioPlayerComponent>()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun onCreate() {
    super.onCreate()
    player = component.exoPlayerFactory.create(this)
    session = MediaSession.Builder(this, player.exoPlayer)
      .setCallback(MediaSessionCallback())
      .build()

    component.audioPlaybackController.currentPlayer.value = player

    // Observe the current time from the player and update the local session with its time
    component.sessionsRepository.observeCurrentSession()
      .flatMapLatest { session ->
        session?.let {
          player.currentTime
            .onEach { currentTime ->
              component.sessionsRepository.updateSession(it.libraryItem.id, currentTime)
            }
        } ?: emptyFlow()
      }
      .launchIn(serviceScope)

    ensureNotificationChannel(NotificationManagerCompat.from(this))
    setListener(MediaSessionServiceListener())
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = session?.player!!
    if (
      !player.playWhenReady
      || player.mediaItemCount == 0
      || player.playbackState == Player.STATE_ENDED
    ) {
      // Stop the service if not playing, continue playing in the background
      // otherwise.
      stopSelf()
    }
  }

  override fun onDestroy() {
    serviceScope.cancel()
    player.release()
    session?.run {
      player.release()
      release()
      session = null
    }
    clearListener()
    component.audioPlaybackController.currentPlayer.value = null
    super.onDestroy()
  }

  private inner class MediaSessionServiceListener : Listener {
    override fun onForegroundServiceStartNotAllowedException() {
      if (
        Build.VERSION.SDK_INT >= 33 &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
      ) {
        // Notification permission is required but not granted
        return
      }
      val notificationManagerCompat = NotificationManagerCompat.from(this@AudioPlayerService)
      ensureNotificationChannel(notificationManagerCompat)
      val builder =
        NotificationCompat.Builder(this@AudioPlayerService, CHANNEL_ID)
          .setSmallIcon(R.drawable.notification_book_icon)
          .setContentTitle(getString(R.string.notification_content_title))
          .setStyle(
            NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_content_text))
          )
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setAutoCancel(true)
//          .also { builder -> getBackStackedActivity()?.let { builder.setContentIntent(it) } }
      notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
    }
  }

  private inner class MediaSessionCallback : MediaSession.Callback {
    override fun onConnect(
      session: MediaSession,
      controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
      return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
        .setAvailablePlayerCommands(
          MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            .buildUpon()
            .build()
        )
        .build()
    }
  }

  private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
    if (
      notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null
    ) {
      return
    }

    val channel =
      NotificationChannel(
        CHANNEL_ID,
        getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
      )
    notificationManagerCompat.createNotificationChannel(channel)
  }

  companion object {
    private const val CHANNEL_ID = "app.campfire.notifications.playback"
    private const val NOTIFICATION_ID = 100

    fun start(context: Context) {
      context.startForegroundService(context.serviceIntent())
    }

    fun stop(context: Context) {
      context.stopService(context.serviceIntent())
    }

    private fun Context.serviceIntent(): Intent = Intent(this, AudioPlayerService::class.java)
  }
}
