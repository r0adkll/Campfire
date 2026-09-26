// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.LocalServerSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = LocalServerSettings::class)
@Inject
class LocalServerSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : LocalServerSettings, AppSettings() {

  private val avoidMobileDataProperty = booleanSetting(KEY_AVOID_MOBILE_DATA, true)
  override var avoidMobileData: Boolean by avoidMobileDataProperty
  override fun observeAvoidMobileData(): StateFlow<Boolean> = avoidMobileDataProperty.observe()
}

// Kept from the earlier "pause away from home" switch so an existing choice carries over
internal const val KEY_AVOID_MOBILE_DATA = "pref_pause_away_from_home"
