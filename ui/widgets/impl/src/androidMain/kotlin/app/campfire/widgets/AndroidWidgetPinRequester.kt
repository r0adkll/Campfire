package app.campfire.widgets

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import app.campfire.core.di.AppScope
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class AndroidWidgetPinRequester(
  private val application: Application,
  private val settings: CampfireSettings,
) : WidgetPinRequester {

  override fun requestPinWidget() {
    val appWidgetManager = AppWidgetManager.getInstance(application)
    if (appWidgetManager.isRequestPinAppWidgetSupported && !settings.hasShownWidgetPinning) {
      val playerWidgetComponent = ComponentName(application, PlayerWidgetReceiver::class.java)
      appWidgetManager.requestPinAppWidget(playerWidgetComponent, null, null)
      settings.hasShownWidgetPinning = true
    }
  }
}
