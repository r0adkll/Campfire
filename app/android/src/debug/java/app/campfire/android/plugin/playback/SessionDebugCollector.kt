package app.campfire.android.plugin.playback

import android.os.Bundle
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * App-agnostic collection points for the plugin's Controllers tab. The integrating
 * app pushes data in from its own session/player wiring:
 *
 * - add [analyticsListener] to your ExoPlayer via `addAnalyticsListener`
 * - call [attachSession]/[detachSession] around your MediaLibrarySession lifecycle
 * - call the record* methods from your MediaSession.Callback overrides
 *
 * The plugin never reaches into the app — only media3 types cross this boundary.
 */
object SessionDebugCollector {

  data class SessionEvent(
    val timeMs: Long,
    val type: String,
    val packageName: String,
    val details: String,
  )

  private val _session = MutableStateFlow<MediaLibrarySession?>(null)
  val session: StateFlow<MediaLibrarySession?> = _session.asStateFlow()

  private val _events = MutableStateFlow<List<SessionEvent>>(emptyList())
  val events: StateFlow<List<SessionEvent>> = _events.asStateFlow()

  private val _analyticsEvents = MutableStateFlow<List<SessionEvent>>(emptyList())
  val analyticsEvents: StateFlow<List<SessionEvent>> = _analyticsEvents.asStateFlow()

  fun attachSession(session: MediaLibrarySession) {
    _session.value = session
    record("session", "-", "MediaLibrarySession attached")
  }

  fun detachSession() {
    _session.value = null
    record("session", "-", "MediaLibrarySession detached")
  }

  fun recordControllerConnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
    record("connect", controller.packageName, describe(session, controller))
  }

  fun recordCustomCommand(packageName: String, action: String, args: Bundle) {
    val argsText = args.keySet().joinToString { key -> "$key=${args.get(key)}" }
    record("customCommand", packageName, "$action($argsText)")
  }

  fun recordMediaButtonEvent(packageName: String, keyCode: Int?) {
    record("mediaButton", packageName, "keyCode=$keyCode")
  }

  fun describe(session: MediaSession, controller: MediaSession.ControllerInfo): String {
    return buildList {
      if (session.isMediaNotificationController(controller)) add("notification")
      if (session.isAutoCompanionController(controller)) add("autoCompanion")
      if (controller.connectionHints.containsKey(CONNECTION_HINT_LIVEWIRE)) add("livewire")
      add("uid=${controller.uid}")
      add("version=${controller.controllerVersion}")
    }.joinToString()
  }

  fun clear() {
    _events.value = emptyList()
  }

  fun clearAnalytics() {
    _analyticsEvents.value = emptyList()
  }

  private fun record(type: String, packageName: String, details: String) {
    _events.update { current ->
      val event = SessionEvent(System.currentTimeMillis(), type, packageName, details)
      (listOf(event) + current).take(MAX_EVENTS)
    }
  }

  private fun recordAnalytics(type: String, details: String) {
    _analyticsEvents.update { current ->
      val event = SessionEvent(System.currentTimeMillis(), type, "-", details)
      (listOf(event) + current).take(MAX_EVENTS)
    }
  }

  /**
   * Attach to your ExoPlayer (`exoPlayer.addAnalyticsListener(...)`) to feed the
   * analytics log. The plugin never holds the player itself.
   */
  val analyticsListener: AnalyticsListener = @UnstableApi
  object : AnalyticsListener {
    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
      val name = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> state.toString()
      }
      recordAnalytics("stateChanged", name)
    }

    override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
      recordAnalytics("isPlaying", isPlaying.toString())
    }

    override fun onMediaItemTransition(eventTime: AnalyticsListener.EventTime, mediaItem: MediaItem?, reason: Int) {
      val reasonName = when (reason) {
        Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "AUTO"
        Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "SEEK"
        Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "REPEAT"
        Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "PLAYLIST_CHANGED"
        else -> reason.toString()
      }
      recordAnalytics("mediaItemTransition", "${mediaItem?.mediaId ?: "null"} reason=$reasonName")
    }

    override fun onPlaybackParametersChanged(
      eventTime: AnalyticsListener.EventTime,
      playbackParameters: PlaybackParameters,
    ) {
      recordAnalytics("playbackParameters", "speed=${playbackParameters.speed} pitch=${playbackParameters.pitch}")
    }

    override fun onAudioDecoderInitialized(
      eventTime: AnalyticsListener.EventTime,
      decoderName: String,
      initializedTimestampMs: Long,
      initializationDurationMs: Long,
    ) {
      recordAnalytics("audioDecoderInitialized", "$decoderName (${initializationDurationMs}ms)")
    }

    override fun onAudioInputFormatChanged(
      eventTime: AnalyticsListener.EventTime,
      format: Format,
      decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
      recordAnalytics(
        "audioInputFormat",
        "${format.sampleMimeType} ${format.sampleRate}Hz ch=${format.channelCount} bitrate=${format.bitrate}",
      )
    }

    override fun onAudioUnderrun(
      eventTime: AnalyticsListener.EventTime,
      bufferSize: Int,
      bufferSizeMs: Long,
      elapsedSinceLastFeedMs: Long,
    ) {
      recordAnalytics("audioUnderrun", "bufferSize=$bufferSize (${bufferSizeMs}ms)")
    }

    override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
      recordAnalytics("audioSessionId", audioSessionId.toString())
    }

    override fun onLoadError(
      eventTime: AnalyticsListener.EventTime,
      loadEventInfo: LoadEventInfo,
      mediaLoadData: MediaLoadData,
      error: IOException,
      wasCanceled: Boolean,
    ) {
      recordAnalytics("loadError", "${error::class.simpleName}: ${error.message} (canceled=$wasCanceled)")
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
      recordAnalytics("playerError", "${error.errorCodeName}: ${error.message}")
    }

    override fun onPositionDiscontinuity(
      eventTime: AnalyticsListener.EventTime,
      oldPosition: Player.PositionInfo,
      newPosition: Player.PositionInfo,
      reason: Int,
    ) {
      val reasonName = when (reason) {
        Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> "AUTO_TRANSITION"
        Player.DISCONTINUITY_REASON_SEEK -> "SEEK"
        Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "SEEK_ADJUSTMENT"
        Player.DISCONTINUITY_REASON_SKIP -> "SKIP"
        Player.DISCONTINUITY_REASON_REMOVE -> "REMOVE"
        Player.DISCONTINUITY_REASON_INTERNAL -> "INTERNAL"
        else -> reason.toString()
      }
      recordAnalytics(
        "positionDiscontinuity",
        "$reasonName ${oldPosition.positionMs}ms -> ${newPosition.positionMs}ms",
      )
    }
  }

  private const val MAX_EVENTS = 200
}
