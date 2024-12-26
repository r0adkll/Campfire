package app.campfire.audioplayer.impl

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.api.SessionsRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ContributesTo(UserScope::class)
interface AudioPlayerComponent {
  val audioPlayerHolder: AudioPlayerHolder // AppScope
  val exoPlayerFactory: ExoPlayerAudioPlayer.Factory // UserScope
  val sessionsRepository: SessionsRepository // UserScope
  val playbackSessionManager: PlaybackSessionManager // UserScope
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
      .build()

    // Attach the Android playback implementation to the controller used by other parts of the
    // to access and control playback / session.
    component.audioPlayerHolder.setCurrentPlayer(player)

    // Setup notification management and checks
    ensureNotificationChannel(NotificationManagerCompat.from(this))
    setListener(MediaSessionServiceListener())
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
      // Stop the service if not playing, continue playing in the background
      // otherwise.
      stopSelf()
    }
  }

  override fun onDestroy() {
    bark(LogPriority.INFO) { "AudioPlayerService::onDestroy()" }
    serviceScope.cancel()
    player.release()
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
          .setSmallIcon(R.drawable.notification_book_icon)
          .setContentTitle(getString(R.string.notification_content_title))
          .setStyle(
            NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_content_text)),
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
    ): ConnectionResult {
      val sessionCommands = ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
        .add(SessionCommand(ACTION_PREPARE_SESSION, Bundle.EMPTY))
        .add(SessionCommand(ACTION_CLEAR_SESSION, Bundle.EMPTY))
        .build()

      return AcceptedResultBuilder(session)
        .setAvailableSessionCommands(sessionCommands)
        .build()
    }

    override fun onCustomCommand(
      session: MediaSession,
      controller: MediaSession.ControllerInfo,
      customCommand: SessionCommand,
      args: Bundle,
    ): ListenableFuture<SessionResult> {
      if (customCommand.customAction == ACTION_PREPARE_SESSION) {
        val libraryItemId = args.getString(EXTRA_LIBRARY_ITEM_ID) ?: return sessionResult(SessionError.ERROR_BAD_VALUE)
        val playImmediately = args.getBoolean(EXTRA_PLAY_IMMEDIATELY)
        val chapterId = args.getInt(EXTRA_CHAPTER_ID, UNSET_CHAPTER_ID)

        // Attach the meta data to the action
        session.sessionExtras = Bundle().apply {
          putString(EXTRA_LIBRARY_ITEM_ID, libraryItemId)
        }

        // Launch the manager to pull/create/prepare the session for the given element
        serviceScope.launch {
          component.playbackSessionManager
            .startSession(libraryItemId, playImmediately, chapterId.takeIf { it != UNSET_CHAPTER_ID })
        }

        return sessionResult(SessionResult.RESULT_SUCCESS)
      } else if (customCommand.customAction == ACTION_CLEAR_SESSION) {
        val libraryItemId = args.getString(EXTRA_LIBRARY_ITEM_ID) ?: return sessionResult(SessionError.ERROR_BAD_VALUE)

        serviceScope.launch {
          component.playbackSessionManager.stopSession(libraryItemId)
          withContext(Dispatchers.Main) {
            player.stop()
          }
        }

        return sessionResult(SessionResult.RESULT_SUCCESS)
      }

      return super.onCustomCommand(session, controller, customCommand, args)
    }

    private fun sessionResult(resultCode: Int): ListenableFuture<SessionResult> {
      return Futures.immediateFuture(SessionResult(resultCode))
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
    private const val TAG = "AudioPlayerService"
    private const val ACTION_PREPARE_SESSION = "prepareLibraryItem"
    private const val ACTION_CLEAR_SESSION = "clearSession"
    private const val EXTRA_LIBRARY_ITEM_ID = "libraryItemId"
    private const val EXTRA_PLAY_IMMEDIATELY = "playWhenPrepared"
    private const val EXTRA_CHAPTER_ID = "chapterId"
    private const val CHANNEL_ID = "app.campfire.notifications.playback"
    private const val NOTIFICATION_ID = 100

    private const val UNSET_CHAPTER_ID = -1

    fun start(
      mediaController: MediaController,
      libraryItemId: LibraryItemId,
      playImmediately: Boolean,
      chapterId: Int?,
    ) {
      mediaController.sendCustomCommand(
        SessionCommand(ACTION_PREPARE_SESSION, Bundle.EMPTY),
        bundleOf(
          EXTRA_LIBRARY_ITEM_ID to libraryItemId,
          EXTRA_PLAY_IMMEDIATELY to playImmediately,
          EXTRA_CHAPTER_ID to (chapterId ?: UNSET_CHAPTER_ID),
        ),
      )
    }

    fun stopSession(
      mediaController: MediaController,
      libraryItemId: LibraryItemId,
    ) {
      mediaController.sendCustomCommand(
        SessionCommand(ACTION_CLEAR_SESSION, Bundle.EMPTY),
        bundleOf(
          EXTRA_LIBRARY_ITEM_ID to libraryItemId,
        ),
      )
    }
  }
}
