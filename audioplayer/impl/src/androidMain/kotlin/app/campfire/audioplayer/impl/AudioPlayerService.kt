package app.campfire.audioplayer.impl

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.settings.api.PlaybackSettings
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@ContributesTo(AppScope::class)
interface AudioPlayerComponent {
  val audioPlayerHolder: AudioPlayerHolder // AppScope
  val exoPlayerFactory: ExoPlayerAudioPlayer.Factory // AppScope
  val playbackSettings: PlaybackSettings // AppScope
  val sessionActivityIntentProvider: SessionActivityIntentProvider
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
    bark(LogPriority.INFO) { "AudioPlayerService::onCreate()" }

    // Create ExoPlayer instance and MediaSession instance that encapsulates the background
    // playback on Android.
    player = component.exoPlayerFactory.create(this)
    session = MediaSession.Builder(this, player.exoPlayer)
      .setCallback(MediaSessionCallback())
      .setSessionActivity(
        PendingIntent.getActivity(
          this,
          0,
          component.sessionActivityIntentProvider.provide(),
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
      )
      .build()

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
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

  override fun onTaskRemoved(rootIntent: Intent?) {
    bark(LogPriority.INFO) { "AudioPlayerService::onTaskRemoved($rootIntent)" }
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
              component.sessionActivityIntentProvider.provide(),
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
          )
      notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
    }
  }

  private inner class MediaSessionCallback : MediaSession.Callback {

    override fun onConnect(
      session: MediaSession,
      controller: MediaSession.ControllerInfo,
    ): ConnectionResult {
      val customLayoutCommandButtons = createCustomLayoutCommandButtons()
      val sessionCommands = ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
        .apply {
          customLayoutCommandButtons.forEach { cmd ->
            cmd.sessionCommand?.let(::add)
          }
        }
        .build()

      return AcceptedResultBuilder(session)
        .setAvailableSessionCommands(sessionCommands)
        .setCustomLayout(customLayoutCommandButtons)
        .build()
    }

    override fun onCustomCommand(
      session: MediaSession,
      controller: MediaSession.ControllerInfo,
      customCommand: SessionCommand,
      args: Bundle,
    ): ListenableFuture<SessionResult> {
      return when (customCommand.customAction) {
        CUSTOM_COMMAND_SEEK_BACKWARD -> {
          player.seekBackward()
          Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        CUSTOM_COMMAND_SEEK_FORWARD -> {
          player.seekForward()
          Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        else -> Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
      }
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

  private fun createCustomLayoutCommandButtons(): List<CommandButton> {
    val skipBackIcon = when (component.playbackSettings.backwardTimeMs) {
      5_000L -> CommandButton.ICON_SKIP_BACK_5
      10_000L -> CommandButton.ICON_SKIP_BACK_10
      15_000L -> CommandButton.ICON_SKIP_BACK_15
      30_000L -> CommandButton.ICON_SKIP_BACK_30
      else -> CommandButton.ICON_SKIP_BACK
    }
    val skipForwardIcon = when (component.playbackSettings.forwardTimeMs) {
      5_000L -> CommandButton.ICON_SKIP_FORWARD_5
      10_000L -> CommandButton.ICON_SKIP_FORWARD_10
      15_000L -> CommandButton.ICON_SKIP_FORWARD_15
      30_000L -> CommandButton.ICON_SKIP_FORWARD_30
      else -> CommandButton.ICON_SKIP_FORWARD
    }

    return listOf(
      CommandButton.Builder(skipBackIcon)
        .setDisplayName(getString(R.string.exo_controls_skip_backward))
        .setSessionCommand(SessionCommand(CUSTOM_COMMAND_SEEK_BACKWARD, Bundle.EMPTY))
        .build(),
      CommandButton.Builder(skipForwardIcon)
        .setDisplayName(getString(R.string.exo_controls_skip_forward))
        .setSessionCommand(SessionCommand(CUSTOM_COMMAND_SEEK_FORWARD, Bundle.EMPTY))
        .build(),
    )
  }

  companion object {
    private const val CHANNEL_ID = "app.campfire.notifications.playback"
    private const val NOTIFICATION_ID = 100

    private const val CUSTOM_COMMAND_SEEK_FORWARD = "app.campfire.media3.SEEK_FORWARD"
    private const val CUSTOM_COMMAND_SEEK_BACKWARD = "app.campfire.media3.SEEK_BACKWARD"
  }
}
