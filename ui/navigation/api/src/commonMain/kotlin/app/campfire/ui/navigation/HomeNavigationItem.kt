// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.slack.circuit.runtime.screen.Screen

@Immutable
data class HomeNavigationItem(
  val screen: Screen,
  val label: String,
  val contentDescription: String,
  val iconImageVector: ImageVector,
  val selectedImageVector: ImageVector? = null,
  /** Optional count rendered as a badge on the icon. Zero (the default) hides the badge. */
  val badgeCount: Int = 0,
  /**
   * Whether a dot marks something waiting behind the destination, e.g. an app update behind
   * Settings. A [badgeCount] takes its place.
   */
  val badged: Boolean = false,
)
