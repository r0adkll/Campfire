package app.campfire.widgets

import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class IosWidgetUpdater : WidgetUpdater {
  override suspend fun updatePlayerWidget(
    currentTime: Duration?,
    currentDuration: Duration?,
    playbackSpeed: Float?,
  ) {
    // Do nothing.
  }
}
