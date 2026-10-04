// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

/**
 * How the app's window layout was last arranged on this device.
 */
interface LayoutSettings {

  /** Whether the desktop wide navigation rail shows labels beside its icons (expanded) or only icons. */
  val wideNavigationRailExpanded: Boolean
  fun setWideNavigationRailExpanded(value: Boolean)
  fun observeWideNavigationRailExpanded(): StateFlow<Boolean>

  /**
   * Width in dp the user dragged the desktop supporting (detail) pane to, or `0` when it has
   * never been resized and the layout's size-class default applies.
   */
  val supportingPaneWidth: Float
  fun setSupportingPaneWidth(value: Float)
  fun observeSupportingPaneWidth(): StateFlow<Float>
}
