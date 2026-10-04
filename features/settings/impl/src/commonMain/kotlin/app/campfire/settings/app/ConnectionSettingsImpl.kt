// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.app

import app.campfire.core.di.AppScope
import app.campfire.settings.api.ConnectionSettings
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ConnectionSettings>())
@Inject
class ConnectionSettingsImpl(
  override val store: SettingsStore,
) : ConnectionSettings, AppSettings() {

  private val socketEnabledProperty = booleanSetting(KEY_SOCKET_ENABLED, true)
  override fun setSocketEnabled(value: Boolean) = socketEnabledProperty.set(value)
  override fun observeSocketEnabled(): Flow<Boolean> = socketEnabledProperty.observe()
}

internal const val KEY_SOCKET_ENABLED = "pref_socket_enabled"
