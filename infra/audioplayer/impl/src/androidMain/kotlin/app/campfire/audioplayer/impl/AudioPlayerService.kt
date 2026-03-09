package app.campfire.audioplayer.impl

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.KeyEvent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.SessionError
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.impl.browse.MediaTree
import app.campfire.audioplayer.impl.browse.SuspendingMediaLibrarySessionCallback
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.infra.audioplayer.impl.R
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.PlaybackSettings
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future

@ContributesTo(AppScope::class)
interface AudioPlayerComponent {
  val audioPlayerHolder: AudioPlayerHolder // AppScope
  val exoPlayerFactory: ExoPlayerAudioPlayer.Factory // AppScope
  val playbackSettings: PlaybackSettings // AppScope
  val activityIntentProvider: ActivityIntentProvider // AppScope
}

@ContributesTo(UserScope::class)
interface AudioPlayerUserComponent {
  val mediaTree: MediaTree
  val sessionsRepository: SessionsRepository
  val playbackSessionManager: PlaybackSessionManager
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
    session = MediaLibrarySession.Builder(this, player.remoteControlPlayer, MediaSessionCallback())
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

  private inner class MediaSessionCallback : SuspendingMediaLibrarySessionCallback(serviceScope) {

    override fun onConnect(
      session: MediaSession,
      controller: MediaSession.ControllerInfo,
    ): ConnectionResult {
      if (
        session.isMediaNotificationController(controller) ||
        session.isAutoCompanionController(controller)
      ) {
        val mediaButtonPreferences = listOf(
          CommandButton.Builder(CommandButton.ICON_PREVIOUS)
            .setDisplayName(getString(R.string.exo_controls_skip_previous))
            .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
            .setSlots(CommandButton.SLOT_BACK)
            .build(),
          CommandButton.Builder(CommandButton.ICON_NEXT)
            .setDisplayName(getString(R.string.exo_controls_skip_next))
            .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
            .setSlots(CommandButton.SLOT_FORWARD)
            .build(),
          *createCustomLayoutCommandButtons().toTypedArray(),
        )

        return AcceptedResultBuilder(session)
          .setMediaButtonPreferences(mediaButtonPreferences)
          .build()
      } else {
        return super.onConnect(session, controller)
      }
    }

    override suspend fun onPlaybackResumptionInternal(
      mediaSession: MediaSession,
      controller: MediaSession.ControllerInfo,
      isForPlayback: Boolean,
    ): MediaSession.MediaItemsWithStartPosition {
      bark(LogPriority.INFO) {
        "onPlaybackResumptionInternal(controller=${controller.packageName}, isForPlayback=$isForPlayback)"
      }

      userComponent.sessionsRepository.getCurrentSession()?.let { session ->
        userComponent.playbackSessionManager.startSession(session.libraryItem.id, playImmediately = isForPlayback)
      }

      // Return an error from this response as we've taken responsibility for starting playback and
      // resolving / setting the media item(s).
      error("Deliberately not return here")
    }

    override fun onMediaButtonEvent(
      session: MediaSession,
      controllerInfo: MediaSession.ControllerInfo,
      intent: Intent,
    ): Boolean {
      // Handle Bluetooth next/prev based on user settings.
      // Media3 routes Bluetooth key events through this callback before processing them,
      // allowing us to intercept and redirect next/prev to seek when the setting is disabled.
      if (controllerInfo.packageName in BLUETOOTH_PACKAGE_NAMES &&
        !component.playbackSettings.remoteNextPrevSkipsChapters
      ) {
        val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
        if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
          when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
              player.seekForward()
              return true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
              player.seekBackward()
              return true
            }
          }
        }
      }
      return super.onMediaButtonEvent(session, controllerInfo, intent)
    }

    override suspend fun onGetLibraryRootInternal(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      params: LibraryParams?,
    ): LibraryResult<MediaItem> {
      return LibraryResult.ofItem(userComponent.mediaTree.root, params)
    }

    override suspend fun onGetChildrenInternal(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      parentId: String,
      page: Int,
      pageSize: Int,
      params: LibraryParams?,
    ): LibraryResult<ImmutableList<MediaItem>> {
      val children = userComponent.mediaTree.getChildren(parentId, page, pageSize)
      if (children.isNotEmpty()) {
        return LibraryResult.ofItemList(children, params)
      }
      return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
    }

    override suspend fun onGetItemInternal(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      mediaId: String,
    ): LibraryResult<MediaItem> {
      userComponent.mediaTree.getItem(mediaId)?.let {
        return LibraryResult.ofItem(it, null)
      }
      return LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
    }

    override suspend fun onSearchInternal(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      query: String,
      params: LibraryParams?,
    ): LibraryResult<Void> {
      val results = userComponent.mediaTree.search(query)
      session.notifySearchResultChanged(browser, query, results.size, params)
      return LibraryResult.ofVoid()
    }

    override suspend fun onGetSearchResultInternal(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      query: String,
      page: Int,
      pageSize: Int,
      params: LibraryParams?,
    ): LibraryResult<ImmutableList<MediaItem>> {
      return userComponent.mediaTree.search(query).let {
        LibraryResult.ofItemList(it, params)
      }
    }

    override suspend fun onAddMediaItemsInternal(
      mediaSession: MediaSession,
      controller: MediaSession.ControllerInfo,
      mediaItems: MutableList<MediaItem>,
    ): MutableList<MediaItem> {
      return super.onAddMediaItemsInternal(mediaSession, controller, mediaItems)
    }

    override fun onSetMediaItems(
      mediaSession: MediaSession,
      controller: MediaSession.ControllerInfo,
      mediaItems: List<MediaItem>,
      startIndex: Int,
      startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
      if (mediaItems.size == 1) {
        return serviceScope.future {
          userComponent.playbackSessionManager.startSession(mediaItems.first().mediaId)

          // Return an error from this response as we've take responsibility for starting playback and
          // resolving / setting the media item(s).
          error("Deliberately not return here")
        }
      } else {
        return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
      }
    }

    override suspend fun onSetMediaItemsInternal(
      mediaSession: MediaSession,
      controller: MediaSession.ControllerInfo,
      mediaItems: List<MediaItem>,
      startIndex: Int,
      startPositionMs: Long,
    ): MediaSession.MediaItemsWithStartPosition {
      val resolvedItems = mediaItems.flatMap { item ->
        if (item.localConfiguration == null) {
          userComponent.mediaTree.resolveMediaItem(item.mediaId)
        } else {
          listOf(item)
        }
      }

      if (resolvedItems.none { it.localConfiguration == null }) {
        return MediaSession.MediaItemsWithStartPosition(resolvedItems, startIndex, startPositionMs)
      } else {
        error("Media items contain an unplayable item!")
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
        .setPlayerCommand(Player.COMMAND_SEEK_BACK)
        .setSlots(CommandButton.SLOT_OVERFLOW)
        .build(),
      CommandButton.Builder(skipForwardIcon)
        .setDisplayName(getString(R.string.exo_controls_skip_forward))
        .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
        .setSlots(CommandButton.SLOT_OVERFLOW)
        .build(),
    )
  }

  companion object {
    private const val CHANNEL_ID = "app.campfire.notifications.playback"
    private const val NOTIFICATION_ID = 100

    /**
     * List of known package names used by bluetooth devices
     */
    private val BLUETOOTH_PACKAGE_NAMES = arrayOf(
      "com.google.android.bluetooth",
      // Pixel Buds use this package name when triggering next/previous actions
      "com.google.android.googlequicksearchbox",
    )
  }
}
