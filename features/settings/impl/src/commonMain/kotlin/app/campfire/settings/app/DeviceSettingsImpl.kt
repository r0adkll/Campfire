// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.app

import app.campfire.core.di.AppScope
import app.campfire.core.model.UserId
import app.campfire.settings.api.DeviceSettings
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<DeviceSettings>())
@Inject
class DeviceSettingsImpl(
  override val store: SettingsStore,
) : DeviceSettings, AppSettings() {

  @OptIn(ExperimentalUuidApi::class)
  private val deviceIdProperty = generatedStringSetting(KEY_DEVICE_ID) { Uuid.random().toString() }
  override suspend fun deviceId(): String = deviceIdProperty.get()

  @OptIn(ExperimentalUuidApi::class)
  private val analyticsIdProperty = generatedStringSetting(KEY_ANALYTICS_ID) { Uuid.random().toString() }
  override suspend fun analyticsId(): String = analyticsIdProperty.get()

  private val currentUserIdProperty = stringOrNullSetting(KEY_CURRENT_USER_ID)
  override fun setCurrentUserId(value: UserId?) = currentUserIdProperty.set(value)
  override fun observeCurrentUserId(): Flow<UserId?> = currentUserIdProperty.observe()
}

internal const val KEY_DEVICE_ID = "pref_device_id"
internal const val KEY_ANALYTICS_ID = "pref_analytics_id"
internal const val KEY_CURRENT_USER_ID = "pref_current_user_id"
