// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.UserId
import app.campfire.settings.api.DeviceSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<DeviceSettings>())
@Inject
class DeviceSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : DeviceSettings, AppSettings() {

  @OptIn(ExperimentalUuidApi::class)
  override var deviceId: String by stringSetting(KEY_DEVICE_ID) { Uuid.random().toString() }

  override var analyticsId: String by stringSetting(KEY_ANALYTICS_ID) { Uuid.random().toString() }

  private val currentUserIdProperty = stringOrNullSetting(KEY_CURRENT_USER_ID)
  override var currentUserId: UserId? by currentUserIdProperty
  override fun observeCurrentUserId(): StateFlow<UserId?> = currentUserIdProperty.observe()
}

internal const val KEY_DEVICE_ID = "pref_device_id"
internal const val KEY_ANALYTICS_ID = "pref_analytics_id"
internal const val KEY_CURRENT_USER_ID = "pref_current_user_id"
