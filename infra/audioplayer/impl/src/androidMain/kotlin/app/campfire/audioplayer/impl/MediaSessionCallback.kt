package app.campfire.audioplayer.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import app.campfire.audioplayer.impl.browse.BrowseMediaId
import app.campfire.audioplayer.impl.browse.SuspendingMediaLibrarySessionCallback
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.infra.audioplayer.impl.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future

@SuppressLint("UnsafeOptInUsageError")
internal class MediaSessionCallback(
  private val context: Context,
  private val serviceScope: CoroutineScope,
  private val player: ExoPlayerAudioPlayer,
  private val component: AudioPlayerComponent,
  private val userComponent: AudioPlayerUserComponent,
) : SuspendingMediaLibrarySessionCallback(serviceScope) {

  private val cycleSpeedCommand = SessionCommand(WidgetSessionCommand.CYCLE_SPEED, Bundle.EMPTY)
  private val sleepTimerCommand = SessionCommand(WidgetSessionCommand.SET_SLEEP_TIMER, Bundle.EMPTY)
  private val clearSleepTimerCommand = SessionCommand(WidgetSessionCommand.CLEAR_SLEEP_TIMER, Bundle.EMPTY)

  override fun onConnect(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
  ): ConnectionResult {
    AudioPlayerDebugHooks.Holder.hooks.onControllerConnected(session, controller)

    val availableCommands = ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
      .add(cycleSpeedCommand)
      .add(sleepTimerCommand)
      .add(clearSleepTimerCommand)
      .build()

    if (
      session.isMediaNotificationController(controller) ||
      session.isAutoCompanionController(controller)
    ) {
      val mediaButtonPreferences = listOf(
        CommandButton.Builder(CommandButton.ICON_PREVIOUS)
          .setDisplayName(context.getString(R.string.exo_controls_skip_previous))
          .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
          .setSlots(CommandButton.SLOT_BACK)
          .build(),
        CommandButton.Builder(CommandButton.ICON_NEXT)
          .setDisplayName(context.getString(R.string.exo_controls_skip_next))
          .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
          .setSlots(CommandButton.SLOT_FORWARD)
          .build(),
        *createCustomLayoutCommandButtons().toTypedArray(),
      )

      return AcceptedResultBuilder(session)
        .setAvailableSessionCommands(availableCommands)
        .setMediaButtonPreferences(mediaButtonPreferences)
        .build()
    } else {
      return AcceptedResultBuilder(session)
        .setAvailableSessionCommands(availableCommands)
        .build()
    }
  }

  override fun onCustomCommand(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
    customCommand: SessionCommand,
    args: Bundle,
  ): ListenableFuture<SessionResult> {
    AudioPlayerDebugHooks.Holder.hooks.onCustomCommand(controller, customCommand.customAction, args)

    when (customCommand.customAction) {
      WidgetSessionCommand.CYCLE_SPEED -> {
        val rates = component.playbackSettings.playbackRates
        val currentSpeed = component.playbackSettings.playbackSpeed
        val currentIndex = rates.indexOfFirst { it == currentSpeed }
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % rates.size
        player.setPlaybackSpeed(rates[nextIndex])
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
      }
      WidgetSessionCommand.SET_SLEEP_TIMER -> {
        val minutes = args.getInt(WidgetSessionCommand.ARG_TIMER_MINUTES, 15).minutes
        player.setTimer(PlaybackTimer.Epoch(minutes.inWholeMilliseconds))
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
      }
      WidgetSessionCommand.CLEAR_SLEEP_TIMER -> {
        player.clearTimer()
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
      }
      else -> return super.onCustomCommand(session, controller, customCommand, args)
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
      userComponent.playbackSessionManager.startSession(
        libraryItemId = session.libraryItem.id,
        playImmediately = isForPlayback,
        episodeId = session.episodeId,
      )
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
    val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)

    if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
      AudioPlayerDebugHooks.Holder.hooks.onMediaButtonEvent(controllerInfo.packageName, keyEvent.keyCode)
    }

    // Record any skip-next / skip-previous package so unknown Bluetooth / remote control
    // senders can be surfaced in developer settings and added to the intercept list.
    if (keyEvent?.action == KeyEvent.ACTION_DOWN &&
      (
        keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
          keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
        )
    ) {
      component.devSettings.recordMediaButtonPackage(controllerInfo.packageName)
    }

    // Handle Bluetooth next/prev based on user settings.
    // Media3 routes Bluetooth key events through this callback before processing them,
    // allowing us to intercept and redirect next/prev to seek when the setting is disabled.
    if (controllerInfo.packageName in BLUETOOTH_PACKAGE_NAMES &&
      !component.playbackSettings.remoteNextPrevSkipsChapters
    ) {
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
    // An empty page past the first is a valid end-of-pagination signal, not an error.
    if (children.isNotEmpty() || page > 0) {
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
    return userComponent.mediaTree.getSearchResults(query, page, pageSize).let {
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
        // Podcast episode entries from the media tree encode their episode id into the
        // mediaId, so decode it to start the session against the right episode.
        val browseId = BrowseMediaId.decode(mediaItems.first().mediaId)
        userComponent.playbackSessionManager.startSession(
          libraryItemId = browseId.libraryItemId,
          episodeId = browseId.episodeId,
        )

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
        .setDisplayName(context.getString(R.string.exo_controls_skip_backward))
        .setPlayerCommand(Player.COMMAND_SEEK_BACK)
        .setSlots(CommandButton.SLOT_OVERFLOW)
        .build(),
      CommandButton.Builder(skipForwardIcon)
        .setDisplayName(context.getString(R.string.exo_controls_skip_forward))
        .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
        .setSlots(CommandButton.SLOT_OVERFLOW)
        .build(),
    )
  }

  private companion object {
    /**
     * List of known package names used by bluetooth devices
     */
    private val BLUETOOTH_PACKAGE_NAMES = arrayOf(
      "com.android.bluetooth",
      "com.google.android.bluetooth",
      // Google Bluetooth APEX services (renamed package in newer Android versions)
      "com.google.android.btservices",
      // Pixel Buds use this package name when triggering next/previous actions
      "com.google.android.googlequicksearchbox",
      // Android Auto sometimes sends the media events to the device.
      "com.google.android.projection.gearhead",
    )
  }
}
