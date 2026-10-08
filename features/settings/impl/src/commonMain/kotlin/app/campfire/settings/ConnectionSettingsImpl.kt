// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.ConnectionSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ConnectionSettings>())
@Inject
class ConnectionSettingsImpl(
  @ForScope(AppScope::class) override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : ConnectionSettings, AppSettings() {

  private val socketEnabledProperty = booleanSetting(KEY_SOCKET_ENABLED, true)
  override var socketEnabled: Boolean by socketEnabledProperty
  override fun observeSocketEnabled(): StateFlow<Boolean> = socketEnabledProperty.observe()
}

internal const val KEY_SOCKET_ENABLED = "pref_socket_enabled"
