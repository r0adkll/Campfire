package app.campfire.audioplayer.impl

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
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
import kotlinx.coroutines.launch

@ContributesTo(UserScope::class)
interface AudioPlayerComponent {
  val audioPlaybackController: AndroidPlaybackController
  val exoPlayerFactory: ExoPlayerAudioPlayer.Factory
  val sessionsRepository: SessionsRepository
  val playbackSessionManager: PlaybackSessionManager
}

@SuppressLint("UnsafeOptInUsageError")
class AudioPlayerService : MediaSessionService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private lateinit var player: ExoPlayerAudioPlayer
  private var session: MediaSession? = null

  private val component by lazy {
    ComponentHolder.component<AudioPlayerComponent>()
  }

  override fun onCreate() {
    super.onCreate()
    // Create ExoPlayer instance and MediaSession instance that encapsulates the background
    // playback on Android.
    player = component.exoPlayerFactory.create(this)
    session = MediaSession.Builder(this, player.exoPlayer)
      .setCallback(MediaSessionCallback())
      .build()

    // Attach the Android playback implementation to the controller used by other parts of the
    // to access and control playback / session.
    component.audioPlaybackController.currentPlayer.value = player

    // Setup notification management and checks
    ensureNotificationChannel(NotificationManagerCompat.from(this))
    setListener(MediaSessionServiceListener())
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val libraryItemId = intent?.getStringExtra(EXTRA_LIBRARY_ITEM_ID)
    if (libraryItemId != null) {
      // Apply the metadata to this current session
      session?.sessionExtras = Bundle().apply {
        putString(EXTRA_LIBRARY_ITEM_ID, libraryItemId)
      }

      // Launch the manager to pull/create/prepare the session for the given element
      serviceScope.launch {
        component.playbackSessionManager.startSession(libraryItemId)
      }
    }

    return super.onStartCommand(intent, flags, startId)
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
    stopCurrentPlaybackSession()
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

  private fun stopCurrentPlaybackSession() {
    val libraryItemId = session?.sessionExtras?.getString(EXTRA_LIBRARY_ITEM_ID)
    if (libraryItemId != null) {
      serviceScope.launch {
        component.playbackSessionManager.stopSession(libraryItemId)
      }
    } else {
      bark(LogPriority.ERROR) {
        "Stopping AudioPlayerService, but no active session was found"
      }
    }
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
    private const val EXTRA_LIBRARY_ITEM_ID = "libraryItemId"
    private const val CHANNEL_ID = "app.campfire.notifications.playback"
    private const val NOTIFICATION_ID = 100

    fun start(
      context: Context,
      libraryItemId: LibraryItemId,
    ) {
      context.startForegroundService(context.serviceIntent(libraryItemId))
    }

    fun stop(context: Context) {
      context.stopService(context.serviceIntent())
    }

    private fun Context.serviceIntent(libraryItemId: LibraryItemId? = null): Intent {
      return Intent(this, AudioPlayerService::class.java).apply {
        if (libraryItemId != null) {
          putExtra(EXTRA_LIBRARY_ITEM_ID, libraryItemId)
        }
      }
    }
  }
}
