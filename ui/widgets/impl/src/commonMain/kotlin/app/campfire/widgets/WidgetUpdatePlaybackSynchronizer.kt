package app.campfire.widgets

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlin.time.Duration
import kotlin.uuid.Uuid
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class WidgetUpdatePlaybackSynchronizer(
  private val widgetUpdater: WidgetUpdater,
  private val widgetPinRequester: WidgetPinRequester,
  private val fatherTime: FatherTime,
) : PlaybackSynchronizer {

  override suspend fun onStateChanged(
    sessionId: Uuid,
    libraryItemId: LibraryItemId,
    state: AudioPlayer.State,
    previousState: AudioPlayer.State,
  ) {
    widgetUpdater.updatePlayerWidget()

    // Prompt the user to pin the playback widget if they haven't seen it yet
    if (previousState != AudioPlayer.State.Playing && state == AudioPlayer.State.Playing) {
      widgetPinRequester.requestPinWidget()
    }
  }

  override suspend fun onMetadataChanged(
    libraryItemId: LibraryItemId,
    metadata: Metadata,
  ) {
    widgetUpdater.updatePlayerWidget()
  }

  var lastCurrentTimeUpdate = 0L
  override suspend fun onCurrentTimeChanged(
    libraryItemId: LibraryItemId,
    currentTime: Duration,
  ) {
    // Updating this too frequently is a performance concern
    // Limit this update to every 2 seconds
    val elapsed = (fatherTime.nowInEpochMillis() - lastCurrentTimeUpdate)
    if (elapsed > TIME_UPDATE_INTERVAL_MS) {
      widgetUpdater.updatePlayerWidget(currentTime = currentTime)
      lastCurrentTimeUpdate = fatherTime.nowInEpochMillis()
    }
  }

  override suspend fun onCurrentDurationChanged(
    libraryItemId: LibraryItemId,
    currentDuration: Duration,
  ) {
    widgetUpdater.updatePlayerWidget(
      currentDuration = currentDuration.takeIf { it.isFinite() } ?: Duration.ZERO,
    )
  }

  override suspend fun onPlaybackSpeedChanged(
    libraryItemId: LibraryItemId,
    playbackSpeed: Float,
  ) {
    widgetUpdater.updatePlayerWidget(playbackSpeed = playbackSpeed)
  }

  companion object {
    private const val TIME_UPDATE_INTERVAL_MS = 5000L
  }
}
