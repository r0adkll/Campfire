// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import app.campfire.core.di.AppScope
import app.campfire.settings.api.AppStateSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AndroidWidgetPinRequester(
  private val application: Application,
  private val appStateSettings: AppStateSettings,
) : WidgetPinRequester {

  override fun requestPinWidget() {
    val appWidgetManager = AppWidgetManager.getInstance(application)
    if (appWidgetManager.isRequestPinAppWidgetSupported && !appStateSettings.hasShownWidgetPinning) {
      val playerWidgetComponent = ComponentName(application, PlayerWidgetReceiver::class.java)
      appWidgetManager.requestPinAppWidget(playerWidgetComponent, null, null)
      appStateSettings.setHasShownWidgetPinning(true)
    }
  }
}
