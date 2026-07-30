// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.ThemeSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = ThemeSettings::class)
@Inject
class ThemeSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : ThemeSettings, AppSettings() {

  private val dynamicallyThemeItemDetailProperty = booleanSetting(KEY_ITEM_DETAIL_THEMING, true)
  override var dynamicallyThemeItemDetail: Boolean by dynamicallyThemeItemDetailProperty
  override fun observeDynamicallyThemeItemDetail(): StateFlow<Boolean> = dynamicallyThemeItemDetailProperty.observe()

  private val dynamicallyThemePlaybackProperty = booleanSetting(KEY_PLAYBACK_THEMING, true)
  override var dynamicallyThemePlayback: Boolean by dynamicallyThemePlaybackProperty
  override fun observeDynamicallyThemePlayback(): StateFlow<Boolean> = dynamicallyThemePlaybackProperty.observe()
}

internal const val KEY_ITEM_DETAIL_THEMING = "pref_dynamically_theme_item_detail"
internal const val KEY_PLAYBACK_THEMING = "pref_dynamically_theme_playback"
