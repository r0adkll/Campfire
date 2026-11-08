// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.automotive

import android.app.Application
import app.campfire.automotive.di.AutomotiveAppComponent
import app.campfire.common.logging.AndroidBark
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.LogRedaction
import dev.zacsweers.metro.createGraphFactory

class CampfireAutomotiveApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      LogRedaction.enabled = false
      Heartwood.grow(AndroidBark())
    }

    val component = createGraphFactory<AutomotiveAppComponent.Factory>().create(this)
    ComponentHolder.components += component

    component.startupInitializer.initialize()
  }
}
