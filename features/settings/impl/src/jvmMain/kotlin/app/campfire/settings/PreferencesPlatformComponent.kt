// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.util.prefs.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import okio.Path
import okio.Path.Companion.toPath

actual interface PreferencesPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettings(
    legacyPreferences: Preferences,
    dispatcherProvider: DispatcherProvider,
  ): ObservableSettings {
    val store = openSettingsStore(
      file = desktopSettingsFile(),
      migrations = listOf(JavaPreferencesMigration(legacyPreferences)),
    )
    store.preload()
    // Edits are written in the background; finish them on exit, as java.util.prefs synced on exit
    Runtime.getRuntime().addShutdownHook(Thread({ runBlocking { store.flush() } }, "SettingsFlush"))
    return FlatPreferencesSettings(store, CoroutineScope(SupervisorJob() + dispatcherProvider.computation))
  }
}

/** `~/.config/Campfire/flatprefs/settings.fpb`, beside the java.util.prefs file it replaces. */
internal fun desktopSettingsFile(): Path {
  val userRoot = System.getProperty("java.util.prefs.userRoot", System.getProperty("user.home"))
  return userRoot.toPath() / ".config" / "Campfire" / "flatprefs" / "$SETTINGS_STORE_NAME.fpb"
}
