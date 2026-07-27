package app.campfire.audioplayer.impl

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.Player
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.impl.browse.MediaTree
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.infra.audioplayer.impl.R
import app.campfire.libraries.api.LibraryRepository
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.AndroidAutoSettings
import app.campfire.settings.api.DevSettings
import app.campfire.settings.api.PlaybackSettings
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ContributesTo(AppScope::class)
interface AudioPlayerComponent {
  val audioPlayerHolder: AudioPlayerHolder // AppScope
  val playbackSettings: PlaybackSettings // AppScope
  val devSettings: DevSettings // AppScope
  val activityIntentProvider: ActivityIntentProvider // AppScope
  val exoPlayerFactory: ExoPlayerAudioPlayer.Factory // AppScope
  val androidAutoSettings: AndroidAutoSettings // AppScope
}

@ContributesTo(UserScope::class)
interface AudioPlayerUserComponent {
  val mediaTree: MediaTree
  val sessionsRepository: SessionsRepository
  val playbackSessionManager: PlaybackSessionManager
  val libraryRepository: LibraryRepository
}

@SuppressLint("UnsafeOptInUsageError")
class AudioPlayerService : MediaLibraryService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private lateinit var player: ExoPlayerAudioPlayer
  private var session: MediaLibrarySession? = null

  private val component by lazy {
    ComponentHolder.component<AudioPlayerComponent>()
  }

  private val userComponent by lazy {
    ComponentHolder.component<AudioPlayerUserComponent>()
  }

  override fun onCreate() {
    super.onCreate()
    bark(LogPriority.INFO) { "AudioPlayerService::onCreate()" }

    // Create ExoPlayer instance and MediaSession instance that encapsulates the background
    // playback on Android.
    player = component.exoPlayerFactory.create(this)
    // Use remoteControlPlayer for MediaSession so remote control commands (Bluetooth, car stereo)
    // can be intercepted and handled based on user settings, while in-app UI uses the direct player.
    session = MediaLibrarySession.Builder(
      this,
      player.player,
      MediaSessionCallback(
        context = this,
        serviceScope = serviceScope,
        player = player,
        component = component,
        userComponent = userComponent,
      ),
    )
      .setSessionActivity(
        PendingIntent.getActivity(
          this,
          0,
          component.activityIntentProvider.provide(),
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
      )
      .build()

    // Bind the session to the player so it can identify the source of remote control commands
    // and apply user settings only for external controllers (Bluetooth, car stereo, etc.)
    player.bindSession(session!!)

    AudioPlayerDebugHooks.Holder.hooks.onSessionCreated(session!!)

    // Attach the Android playback implementation to the controller used by other parts of the
    // to access and control playback / session.
    component.audioPlayerHolder.setCurrentPlayer(player)

    // Setup notification management and checks
    ensureNotificationChannel(NotificationManagerCompat.from(this))
    setListener(MediaSessionServiceListener())

    // Customize the media notification provider
    val mediaNotificationProvider = DefaultMediaNotificationProvider.Builder(this)
      .setNotificationId(NOTIFICATION_ID)
      .setChannelId(CHANNEL_ID)
      .build()
      .apply {
        setSmallIcon(R.drawable.ic_notification)
      }
    setMediaNotificationProvider(mediaNotificationProvider)

    observeBrowseTreeInvalidation()
  }

  /**
   * Re-notify connected browsers (Android Auto head units, etc.) when the inputs to the
   * browse tree change — the active library (switching libraries changes the root tabs and
   * every tab's contents) or the Android Auto category settings (order/visibility/layout).
   */
  private fun observeBrowseTreeInvalidation() {
    serviceScope.launch {
      combine(
        userComponent.libraryRepository.observeCurrentLibrary(refresh = false)
          .map { it.id to it.mediaType }
          .distinctUntilChanged(),
        component.androidAutoSettings.observeCategoryConfigs(),
      ) { library, configs -> library to configs }
        // Browsers that connect later query a fresh tree anyway; only notify on change.
        .drop(1)
        .collect {
          // Media3 requires the session be accessed on the thread it was created on.
          withContext(Dispatchers.Main) {
            val session = session ?: return@withContext
            userComponent.mediaTree.invalidationParentIds.forEach { parentId ->
              session.notifyChildrenChanged(parentId, Int.MAX_VALUE, null)
            }
          }
        }
    }
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

  override fun onTaskRemoved(rootIntent: Intent?) {
    bark(LogPriority.INFO) { "AudioPlayerService::onTaskRemoved()" }
    val player = session?.player!!
    if (
      !player.playWhenReady ||
      player.mediaItemCount == 0 ||
      player.playbackState == Player.STATE_ENDED
    ) {
      bark(LogPriority.WARN) { "AudioPlayerService::onTaskRemoved() - Stopping service" }
      // Stop the service if not playing, continue playing in the background
      // otherwise.
      stopSelf()
    }
  }

  override fun onDestroy() {
    bark(LogPriority.INFO) { "AudioPlayerService::onDestroy()" }
    serviceScope.cancel()
    session?.run {
      player.stop()
      player.release()
      release()
      session = null
    }
    AudioPlayerDebugHooks.Holder.hooks.onSessionReleased()
    clearListener()
    component.audioPlayerHolder.release()
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
          .setSmallIcon(R.drawable.ic_notification)
          .setContentTitle(getString(R.string.notification_content_title))
          .setStyle(
            NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_content_text)),
          )
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setAutoCancel(true)
          .setContentIntent(
            PendingIntent.getActivity(
              this@AudioPlayerService,
              0,
              component.activityIntentProvider.provide(),
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
          )
      notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
    }
  }

  private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
    if (notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null) {
      return
    }

    val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
      .setName(getString(R.string.notification_channel_name))
      .setVibrationEnabled(false)
      .build()

    notificationManagerCompat.createNotificationChannel(channel)
  }

  companion object {
    private const val CHANNEL_ID = "app.campfire.notifications.playback"
    private const val NOTIFICATION_ID = 100
  }
}
