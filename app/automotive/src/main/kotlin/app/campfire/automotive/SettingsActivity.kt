// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.automotive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import app.campfire.common.root.SettingsWindowContent
import app.campfire.core.ComponentActivityPlugin
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.settings.DefaultSettingsSections
import app.campfire.settings.api.SettingsSections
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject

/**
 * Campfire's settings in the car. The car's media app opens it from its settings button
 * (`ACTION_APPLICATION_PREFERENCES`), and only while parked. Signed out, it signs in first.
 */
class SettingsActivity : ComponentActivity() {

  private val component by lazy { ComponentHolder.component<SettingsComponent>() }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    component.componentActivityPlugins.forEach { it.register(this) }

    setContent {
      CompositionLocalProvider(
        LocalUriHandler provides CarUriHandler(this),
      ) {
        component.settingsWindowContent(onClose = ::finish)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    component.componentActivityPlugins.forEach { it.unregister() }
  }
}

@ContributesTo(AppScope::class)
interface SettingsComponent {
  val settingsWindowContent: SettingsWindowContent
  val componentActivityPlugins: Set<ComponentActivityPlugin>
}

/** A car's settings: no appearance or book info, and browsing set up for the car's media app. */
@ContributesBinding(AppScope::class, replaces = [DefaultSettingsSections::class])
@Inject
class CarSettingsSections : SettingsSections {
  override val isCar: Boolean = true
}
