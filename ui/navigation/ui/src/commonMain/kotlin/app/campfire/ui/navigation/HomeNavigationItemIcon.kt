// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun HomeNavigationItemIcon(item: HomeNavigationItem, selected: Boolean) {
  WithBadge(item) {
    val selectedImageVector = item.selectedImageVector
    if (selectedImageVector != null) {
      Crossfade(targetState = selected) { s ->
        InProgressIcon(
          imageVector = if (s) selectedImageVector else item.iconImageVector,
          contentDescription = item.contentDescription,
          inProgress = item.inProgress,
        )
      }
    } else {
      InProgressIcon(
        imageVector = item.iconImageVector,
        contentDescription = item.contentDescription,
        inProgress = item.inProgress,
      )
    }
  }
}

@Composable
private fun WithBadge(item: HomeNavigationItem, content: @Composable () -> Unit) {
  if (item.badgeCount > 0) {
    BadgedBox(
      badge = { Badge { Text(badgeCountLabel(item.badgeCount)) } },
    ) {
      content()
    }
  } else {
    WithDotBadge(item.badged, content)
  }
}

/** Marks [content] with a dot while [badged], for something waiting behind it. */
@Composable
internal fun WithDotBadge(badged: Boolean, content: @Composable () -> Unit) {
  if (badged) {
    BadgedBox(badge = { Badge() }) {
      content()
    }
  } else {
    content()
  }
}

/** [count] as a badge shows it, capped like Material's badges so it stays short. */
internal fun badgeCountLabel(count: Int): String = if (count > MaxBadgeCount) "$MaxBadgeCount+" else count.toString()

private const val MaxBadgeCount = 999
