// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun HomeNavigationItemIcon(item: HomeNavigationItem, selected: Boolean) {
  WithBadge(item) {
    val selectedImageVector = item.selectedImageVector
    if (selectedImageVector != null) {
      Crossfade(targetState = selected) { s ->
        Icon(
          imageVector = if (s) selectedImageVector else item.iconImageVector,
          contentDescription = item.contentDescription,
        )
      }
    } else {
      Icon(
        imageVector = item.iconImageVector,
        contentDescription = item.contentDescription,
      )
    }
  }
}

@Composable
private fun WithBadge(item: HomeNavigationItem, content: @Composable () -> Unit) {
  if (item.badgeCount > 0) {
    BadgedBox(
      badge = { Badge { Text(item.badgeCount.toString()) } },
    ) {
      content()
    }
  } else {
    content()
  }
}
