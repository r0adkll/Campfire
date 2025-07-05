package app.campfire.widgets

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.model.LibraryItemId
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlin.time.Duration
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class WidgetUpdatePlaybackSynchronizer(
  private val widgetUpdater: WidgetUpdater,
  private val widgetPinRequester: WidgetPinRequester,
) : PlaybackSynchronizer {

  override suspend fun onStateChanged(
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

  override suspend fun onCurrentTimeChanged(
    libraryItemId: LibraryItemId,
    currentTime: Duration,
  ) {
    widgetUpdater.updatePlayerWidget()
  }

  override suspend fun onCurrentDurationChanged(
    libraryItemId: LibraryItemId,
    currentDuration: Duration,
  ) {
    widgetUpdater.updatePlayerWidget()
  }
}
