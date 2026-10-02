// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.LayoutSettings
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
@ContributesBinding(AppScope::class, binding = binding<LayoutSettings>())
@Inject
class LayoutSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : LayoutSettings, AppSettings() {

  private val wideNavigationRailExpandedProperty = booleanSetting(KEY_WIDE_NAVIGATION_RAIL_EXPANDED, true)
  override var wideNavigationRailExpanded: Boolean by wideNavigationRailExpandedProperty
  override fun observeWideNavigationRailExpanded(): StateFlow<Boolean> = wideNavigationRailExpandedProperty.observe()

  private val supportingPaneWidthProperty = floatSetting(KEY_SUPPORTING_PANE_WIDTH, 0f)
  override var supportingPaneWidth: Float by supportingPaneWidthProperty
  override fun observeSupportingPaneWidth(): StateFlow<Float> = supportingPaneWidthProperty.observe()
}

internal const val KEY_WIDE_NAVIGATION_RAIL_EXPANDED = "pref_wide_navigation_rail_expanded"
internal const val KEY_SUPPORTING_PANE_WIDTH = "pref_supporting_pane_width"
