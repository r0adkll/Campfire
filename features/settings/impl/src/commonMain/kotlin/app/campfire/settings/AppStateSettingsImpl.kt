// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.AppStateSettings
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
@ContributesBinding(AppScope::class, binding = binding<AppStateSettings>())
@Inject
class AppStateSettingsImpl(
  @SettingsStore override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : AppStateSettings, AppSettings() {

  private val hasShownWidgetPinningProperty = booleanSetting(KEY_SHOW_WIDGET_PINNING, false)
  override var hasShownWidgetPinning: Boolean by hasShownWidgetPinningProperty
  override fun observeHasShownWidgetPinning(): StateFlow<Boolean> = hasShownWidgetPinningProperty.observe()

  private val lastSeenVersionProperty = stringOrNullSetting(KEY_LAST_SEEN_WHATS_NEW)
  override var lastSeenVersion: String? by lastSeenVersionProperty
  override fun observeLastSeenVersion(): StateFlow<String?> = lastSeenVersionProperty.observe()

  private val appUpdateSignInDismissedProperty = booleanSetting(KEY_APP_UPDATE_SIGN_IN_DISMISSED, false)
  override var appUpdateSignInDismissed: Boolean by appUpdateSignInDismissedProperty
  override fun observeAppUpdateSignInDismissed(): StateFlow<Boolean> = appUpdateSignInDismissedProperty.observe()

  private val appUpdateDismissedVersionCodeProperty = longSetting(KEY_APP_UPDATE_DISMISSED_VERSION_CODE, 0L)
  override var appUpdateDismissedVersionCode: Long by appUpdateDismissedVersionCodeProperty
  override fun observeAppUpdateDismissedVersionCode(): StateFlow<Long> =
    appUpdateDismissedVersionCodeProperty.observe()
}

internal const val KEY_SHOW_WIDGET_PINNING = "pref_show_widget_pinning"
internal const val KEY_LAST_SEEN_WHATS_NEW = "pref_last_seen_whats_new"
internal const val KEY_APP_UPDATE_SIGN_IN_DISMISSED = "pref_app_update_sign_in_dismissed"
internal const val KEY_APP_UPDATE_DISMISSED_VERSION_CODE = "pref_app_update_dismissed_version_code"
