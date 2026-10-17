// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.ConnectionSettings
import app.campfire.settings.api.OfflineSession
import app.campfire.settings.api.OfflineSession.Companion.toStorageKey
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

  private val offlineSessionProperty = customSetting<OfflineSession>(
    key = KEY_OFFLINE_SESSION,
    defaultValue = OfflineSession.None,
    getter = OfflineSession::fromStorageKey,
    // None stores as "", which reads back as None
    setter = { it.toStorageKey().orEmpty() },
  )
  override var offlineSession: OfflineSession by offlineSessionProperty
  override fun observeOfflineSession(): StateFlow<OfflineSession> = offlineSessionProperty.observe()

  private val workOfflineOnMobileDataProperty = booleanSetting(KEY_WORK_OFFLINE_ON_MOBILE_DATA, false)
  override var workOfflineOnMobileData: Boolean by workOfflineOnMobileDataProperty
  override fun observeWorkOfflineOnMobileData(): StateFlow<Boolean> = workOfflineOnMobileDataProperty.observe()

  private val mobileDataOfflineSuspendedProperty = booleanSetting(KEY_MOBILE_DATA_OFFLINE_SUSPENDED, false)
  override var mobileDataOfflineSuspended: Boolean by mobileDataOfflineSuspendedProperty
  override fun observeMobileDataOfflineSuspended(): StateFlow<Boolean> = mobileDataOfflineSuspendedProperty.observe()
}

internal const val KEY_SOCKET_ENABLED = "pref_socket_enabled"
internal const val KEY_OFFLINE_SESSION = "pref_offline_session"
internal const val KEY_WORK_OFFLINE_ON_MOBILE_DATA = "pref_work_offline_on_mobile_data"
internal const val KEY_MOBILE_DATA_OFFLINE_SUSPENDED = "pref_mobile_data_offline_suspended"
