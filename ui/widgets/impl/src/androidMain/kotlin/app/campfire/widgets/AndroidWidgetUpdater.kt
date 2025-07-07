package app.campfire.widgets

import android.app.Application
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import app.campfire.core.di.AppScope
import app.campfire.core.extensions.asSeconds
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class AndroidWidgetUpdater(
  private val application: Application,
) : WidgetUpdater {

  override suspend fun updatePlayerWidget(
    currentTime: Duration?,
    currentDuration: Duration?,
    playbackSpeed: Float?,
  ) {
    // Update the widget with the new data
    val glanceIds = GlanceAppWidgetManager(application).getGlanceIds(PlayerWidget::class.java)
    glanceIds.forEach { id ->
      updateAppWidgetState(application, id) { state ->
        currentTime?.let { state[PlayerWidget.KEY_CURRENT_TIME] = it.asSeconds() }
        currentDuration?.let { state[PlayerWidget.KEY_CURRENT_DURATION] = it.asSeconds() }
        playbackSpeed?.let { state[PlayerWidget.KEY_PLAYBACK_SPEED] = it }
      }
      PlayerWidget().update(application, id)
    }
  }
}
