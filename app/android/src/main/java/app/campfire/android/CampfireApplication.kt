// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android

import android.app.Application
import androidx.compose.runtime.Composer
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import app.campfire.android.di.AndroidAppComponent
import app.campfire.android.logging.AndroidBark
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.LogRedaction
import app.campfire.tracing.Trace
import app.campfire.tracing.trace
import kimchi.merge.app.campfire.android.di.createAndroidAppComponent

class CampfireApplication : Application() {

  override fun onCreate() = Trace.trace("CampfireApplication.onCreate") {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      // Local logcat only — keep raw URLs readable for development.
      LogRedaction.enabled = false
      Heartwood.grow(AndroidBark())
    }

    // Create application component
    val component = Trace.trace("AppComponent") {
      AndroidAppComponent.createAndroidAppComponent(this).also {
        ComponentHolder.components += it
      }
    }

    // Call startup initializers
    component.startupInitializer.initialize()

    // Enable more useful compose stack traces in crashes
    Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.Auto)
  }
}
