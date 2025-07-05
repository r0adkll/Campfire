package app.campfire.widgets

import android.app.Application
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import app.campfire.core.di.AppScope
import app.campfire.core.time.FatherTime
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class AndroidWidgetUpdater(
  private val application: Application,
  private val fatherTime: FatherTime,
) : WidgetUpdater {

  override suspend fun updatePlayerWidget() {
//    PlayerWidget().updateAll(application)

    // Update the widget with the new data
    val glanceIds = GlanceAppWidgetManager(application).getGlanceIds(PlayerWidget::class.java)
    glanceIds.forEach { id ->
      updateAppWidgetState(application, id) {
        it[longPreferencesKey("now")] = fatherTime.nowInEpochMillis()
      }
      PlayerWidget().update(application, id)
    }
  }
}
