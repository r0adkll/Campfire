package app.campfire.widgets

import android.app.Application
import androidx.glance.appwidget.updateAll
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class AndroidWidgetUpdater(
  private val application: Application,
) : WidgetUpdater {

  override suspend fun updatePlayerWidget() {
    PlayerWidget().updateAll(application)
  }
}
