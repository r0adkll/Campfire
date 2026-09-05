// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.layout

enum class NavigationType {
  BottomNavigation,
  Rail,

  /**
   * A collapsible wide navigation rail that carries every destination the permanent drawer
   * would, for desktop windows that are wide enough for labels but not for a permanent drawer.
   */
  WideRail,
  Drawer,
}
